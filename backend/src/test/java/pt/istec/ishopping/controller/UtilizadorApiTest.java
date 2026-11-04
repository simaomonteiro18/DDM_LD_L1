package pt.istec.ishopping.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;

import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.UtilizadorRepository;
import pt.istec.ishopping.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UtilizadorApiTest {

    private static final String BASE = "/api/utilizadores";
    private static final String PASSWORD_TESTE = "palavra-secreta-de-teste";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private UtilizadorRepository utilizadorRepository;
    @MockitoBean
    private PlatformTransactionManager transactionManager;

    private Utilizador autenticado;
    private String autorizacao;

    @BeforeEach
    void preparar() {
        autenticado = criarUtilizador(1, "ana");
        autorizacao = "Bearer " + jwtService.gerar(autenticado).valor();
        when(utilizadorRepository.findById(1)).thenReturn(Optional.of(autenticado));
    }

    private Utilizador criarUtilizador(Integer id, String username) {
        Utilizador utilizador = new Utilizador();
        ReflectionTestUtils.setField(utilizador, "id", id);
        utilizador.setNome("Nome " + username);
        utilizador.setUsername(username);
        utilizador.setPasswordHash("hash-que-nunca-sai");
        return utilizador;
    }

    private String corpo(String nome, String username, String password) {
        return "{\"nome\":\"" + nome + "\",\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private void esperarSemDadosSensiveis(ResultActions resultado) throws Exception {
        resultado
                .andExpect(content().string(not(containsString("hash-que-nunca-sai"))))
                .andExpect(content().string(not(containsString(PASSWORD_TESTE))))
                .andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString("passwordHash"))))
                .andExpect(content().string(not(containsString("password_hash"))));
    }

    private ResultActions criar(String json) throws Exception {
        return mockMvc.perform(post(BASE).header("Authorization", autorizacao)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void pedidosSemTokenDao401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(BASE + "/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void listarDevolveOrdenadoPorNomeSemDadosSensiveis() throws Exception {
        when(utilizadorRepository.findAll(Sort.by("nome", "id"))).thenReturn(List.of(autenticado));

        ResultActions resultado = mockMvc.perform(get(BASE).header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Nome ana"))
                .andExpect(jsonPath("$[0].username").value("ana"));
        esperarSemDadosSensiveis(resultado);
    }

    @Test
    void obterInexistenteDa404NoFormatoDeErro() throws Exception {
        when(utilizadorRepository.findById(9)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/9").header("Authorization", autorizacao))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.erro").value("O utilizador não existe."))
                .andExpect(jsonPath("$.caminho").value(BASE + "/9"));
    }

    @Test
    void idQueNaoEhNumeroDa400() throws Exception {
        mockMvc.perform(get(BASE + "/abc").header("Authorization", autorizacao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
    }

    @Test
    void criarDevolve201ComLocationSemDadosSensiveis() throws Exception {
        when(utilizadorRepository.findByUsernameIgnoreCase("rui")).thenReturn(Optional.empty());
        when(utilizadorRepository.saveAndFlush(any(Utilizador.class))).thenAnswer(pedido -> {
            Utilizador guardado = pedido.getArgument(0);
            ReflectionTestUtils.setField(guardado, "id", 2);
            return guardado;
        });

        ResultActions resultado = criar(corpo("  Rui Costa ", " rui ", PASSWORD_TESTE))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/2")))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.nome").value("Rui Costa"))
                .andExpect(jsonPath("$.username").value("rui"));
        esperarSemDadosSensiveis(resultado);
    }

    @Test
    void criarComDadosEmFaltaDa400ComMensagensEmPortugues() throws Exception {
        criar("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.erro").value(containsString("O nome é obrigatório.")))
                .andExpect(jsonPath("$.erro").value(containsString("O username é obrigatório.")))
                .andExpect(jsonPath("$.erro").value(containsString("A palavra-passe é obrigatória.")));
    }

    @Test
    void criarComPasswordVaziaDizQueEhObrigatoria() throws Exception {
        criar(corpo("Rui", "rui", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("A palavra-passe é obrigatória."));
    }

    @Test
    void criarComUsernameComEspacosDa400() throws Exception {
        criar(corpo("Rui", "rui costa", PASSWORD_TESTE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O username não pode conter espaços."));
    }

    @Test
    void criarComCamposDemasiadoLongosDa400() throws Exception {
        criar(corpo("n".repeat(101), "u".repeat(51), PASSWORD_TESTE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("mais de 100 caracteres")))
                .andExpect(jsonPath("$.erro").value(containsString("mais de 50 caracteres")));
    }

    @Test
    void passwordCurtaOuAcimaDoLimiteDoBcryptDa400() throws Exception {
        for (String invalida : List.of("12345", "      ", "a".repeat(73), "ã".repeat(40))) {
            ResultActions resultado = criar(corpo("Rui", "rui", invalida))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erro").value(containsString("entre 6 e 72 caracteres")));
            resultado.andExpect(content().string(not(containsString(invalida))));
        }
    }

    @Test
    void jsonMalFormadoDa400() throws Exception {
        criar("{")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
    }

    @Test
    void usernameDuplicadoDa409() throws Exception {
        when(utilizadorRepository.findByUsernameIgnoreCase(anyString())).thenReturn(Optional.of(autenticado));

        criar(corpo("Outra Ana", "ANA", PASSWORD_TESTE))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.erro").value("Já existe um utilizador com este username."));
    }

    @Test
    void atualizarSemPasswordAceitaEDevolve200() throws Exception {
        when(utilizadorRepository.findByUsernameIgnoreCase("ana")).thenReturn(Optional.of(autenticado));
        when(utilizadorRepository.saveAndFlush(any(Utilizador.class))).thenAnswer(pedido -> pedido.getArgument(0));

        ResultActions resultado = mockMvc.perform(put(BASE + "/1").header("Authorization", autorizacao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Ana Nova\",\"username\":\"ana\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Nova"));
        esperarSemDadosSensiveis(resultado);
    }

    @Test
    void apagarDevolve204() throws Exception {
        when(utilizadorRepository.findById(2)).thenReturn(Optional.of(criarUtilizador(2, "rui")));

        mockMvc.perform(delete(BASE + "/2").header("Authorization", autorizacao))
                .andExpect(status().isNoContent());
    }

    @Test
    void apagarOProprioDa409() throws Exception {
        mockMvc.perform(delete(BASE + "/1").header("Authorization", autorizacao))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Não é possível apagar o próprio utilizador autenticado."));
    }
}
