package pt.istec.ishopping.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;

import pt.istec.ishopping.config.InicializadorUtilizador;
import pt.istec.ishopping.model.TipoArtigo;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.TipoArtigoRepository;
import pt.istec.ishopping.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TipoArtigoApiTest {

    private static final String BASE = "/api/tipos-artigo";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private TipoArtigoRepository tipoArtigoRepository;
    @MockitoBean
    private PlatformTransactionManager transactionManager;
    @MockitoBean
    private InicializadorUtilizador inicializadorUtilizador;

    private String autorizacao;

    @BeforeEach
    void preparar() {
        Utilizador utilizador = new Utilizador();
        ReflectionTestUtils.setField(utilizador, "id", 1);
        autorizacao = "Bearer " + jwtService.gerar(utilizador).valor();
    }

    private TipoArtigo criarTipo(Integer id, String nome) {
        TipoArtigo tipoArtigo = new TipoArtigo();
        ReflectionTestUtils.setField(tipoArtigo, "id", id);
        tipoArtigo.setNome(nome);
        return tipoArtigo;
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
    void listarDevolveAEstruturaEsperada() throws Exception {
        when(tipoArtigoRepository.findAll(Sort.by("nome", "id"))).thenReturn(List.of(criarTipo(1, "Bebidas")));

        mockMvc.perform(get(BASE).header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Bebidas"));
    }

    @Test
    void obterInexistenteDa404NoFormatoDeErro() throws Exception {
        when(tipoArtigoRepository.findById(9)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/9").header("Authorization", autorizacao))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.erro").value("O tipo de artigo não existe."))
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
    void criarDevolve201ComLocation() throws Exception {
        when(tipoArtigoRepository.findByNomeIgnoreCase("Bebidas")).thenReturn(Optional.empty());
        when(tipoArtigoRepository.saveAndFlush(any(TipoArtigo.class))).thenAnswer(pedido -> {
            TipoArtigo guardado = pedido.getArgument(0);
            ReflectionTestUtils.setField(guardado, "id", 2);
            return guardado;
        });

        criar("{\"nome\":\"  Bebidas \"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/2")))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.nome").value("Bebidas"));
    }

    @Test
    void criarSemNomeDa400ComMensagemEmPortugues() throws Exception {
        criar("{\"nome\":\"   \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.erro").value("O nome é obrigatório."));
    }

    @Test
    void criarComNomeDemasiadoLongoDa400() throws Exception {
        criar("{\"nome\":\"" + "n".repeat(101) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O nome não pode ter mais de 100 caracteres."));
    }

    @Test
    void jsonMalFormadoDa400() throws Exception {
        criar("{")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
    }

    @Test
    void nomeDuplicadoDa409() throws Exception {
        when(tipoArtigoRepository.findByNomeIgnoreCase("BEBIDAS")).thenReturn(Optional.of(criarTipo(1, "Bebidas")));

        criar("{\"nome\":\"BEBIDAS\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.erro").value("Já existe um tipo de artigo com este nome."));
    }

    @Test
    void atualizarDevolve200() throws Exception {
        TipoArtigo tipoArtigo = criarTipo(1, "Bebidas");
        when(tipoArtigoRepository.findById(1)).thenReturn(Optional.of(tipoArtigo));
        when(tipoArtigoRepository.findByNomeIgnoreCase("Sumos")).thenReturn(Optional.empty());
        when(tipoArtigoRepository.saveAndFlush(tipoArtigo)).thenReturn(tipoArtigo);

        mockMvc.perform(put(BASE + "/1").header("Authorization", autorizacao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Sumos\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Sumos"));
    }

    @Test
    void apagarDevolve204() throws Exception {
        when(tipoArtigoRepository.findById(1)).thenReturn(Optional.of(criarTipo(1, "Bebidas")));

        mockMvc.perform(delete(BASE + "/1").header("Authorization", autorizacao))
                .andExpect(status().isNoContent());
    }

    @Test
    void apagarComArtigosAssociadosDa409() throws Exception {
        when(tipoArtigoRepository.findById(1)).thenReturn(Optional.of(criarTipo(1, "Bebidas")));
        doThrow(new DataIntegrityViolationException("fk")).when(tipoArtigoRepository).flush();

        mockMvc.perform(delete(BASE + "/1").header("Authorization", autorizacao))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Não é possível apagar um tipo de artigo com artigos associados."));
    }
}
