package pt.istec.ishopping.controller;

import static org.hamcrest.Matchers.containsString;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;

import pt.istec.ishopping.config.InicializadorUtilizador;
import pt.istec.ishopping.model.Artigo;
import pt.istec.ishopping.model.TipoArtigo;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.ArtigoRepository;
import pt.istec.ishopping.repository.TipoArtigoRepository;
import pt.istec.ishopping.repository.UtilizadorRepository;
import pt.istec.ishopping.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ArtigoApiTest {

    private static final String BASE = "/api/artigos";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private UtilizadorRepository utilizadorRepository;
    @MockitoBean
    private ArtigoRepository artigoRepository;
    @MockitoBean
    private TipoArtigoRepository tipoArtigoRepository;
    @MockitoBean
    private PlatformTransactionManager transactionManager;
    @MockitoBean
    private InicializadorUtilizador inicializadorUtilizador;

    private String autorizacao;
    private TipoArtigo mercearia;

    @BeforeEach
    void preparar() {
        Utilizador utilizador = new Utilizador();
        ReflectionTestUtils.setField(utilizador, "id", 1);
        when(utilizadorRepository.existsById(1)).thenReturn(true);
        autorizacao = "Bearer " + jwtService.gerar(utilizador).valor();
        mercearia = new TipoArtigo();
        ReflectionTestUtils.setField(mercearia, "id", 2);
        mercearia.setNome("Mercearia");
    }

    private Artigo criarArtigo(Integer id, String nome) {
        Artigo artigo = new Artigo();
        ReflectionTestUtils.setField(artigo, "id", id);
        artigo.setNome(nome);
        artigo.setTipoArtigo(mercearia);
        return artigo;
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
    void listarDevolveOArtigoComOTipo() throws Exception {
        when(artigoRepository.findAllComTipo()).thenReturn(List.of(criarArtigo(1, "Arroz")));

        mockMvc.perform(get(BASE).header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Arroz"))
                .andExpect(jsonPath("$[0].tipo.id").value(2))
                .andExpect(jsonPath("$[0].tipo.nome").value("Mercearia"));
    }

    @Test
    void listarComFiltroUsaOParametro() throws Exception {
        when(artigoRepository.findAllComTipoPorIdTipoArtigo(2)).thenReturn(List.of(criarArtigo(1, "Arroz")));

        mockMvc.perform(get(BASE).param("idTipoArtigo", "2").header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Arroz"));
    }

    @Test
    void filtroQueNaoEhNumeroDa400() throws Exception {
        mockMvc.perform(get(BASE).param("idTipoArtigo", "abc").header("Authorization", autorizacao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
    }

    @Test
    void obterInexistenteDa404NoFormatoDeErro() throws Exception {
        when(artigoRepository.findById(9)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/9").header("Authorization", autorizacao))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.erro").value("O artigo não existe."))
                .andExpect(jsonPath("$.caminho").value(BASE + "/9"));
    }

    @Test
    void idQueNaoEhNumeroDa400() throws Exception {
        mockMvc.perform(get(BASE + "/abc").header("Authorization", autorizacao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));
    }

    @Test
    void criarDevolve201ComLocationEOTipo() throws Exception {
        when(tipoArtigoRepository.findById(2)).thenReturn(Optional.of(mercearia));
        when(artigoRepository.findByIdTipoArtigoAndNomeIgnoreCase(2, "Arroz")).thenReturn(Optional.empty());
        when(artigoRepository.saveAndFlush(any(Artigo.class))).thenAnswer(pedido -> {
            Artigo guardado = pedido.getArgument(0);
            ReflectionTestUtils.setField(guardado, "id", 5);
            return guardado;
        });

        criar("{\"nome\":\"  Arroz \",\"idTipoArtigo\":2}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/5")))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.nome").value("Arroz"))
                .andExpect(jsonPath("$.tipo.id").value(2))
                .andExpect(jsonPath("$.tipo.nome").value("Mercearia"));
    }

    @Test
    void criarComDadosEmFaltaDa400ComMensagensEmPortugues() throws Exception {
        criar("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.erro").value(containsString("O nome é obrigatório.")))
                .andExpect(jsonPath("$.erro").value(containsString("O tipo de artigo é obrigatório.")));
    }

    @Test
    void criarComNomeDemasiadoLongoDa400() throws Exception {
        criar("{\"nome\":\"" + "n".repeat(101) + "\",\"idTipoArtigo\":2}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O nome não pode ter mais de 100 caracteres."));
    }

    @Test
    void criarComIdDeTipoQueNaoEhNumeroDa400() throws Exception {
        criar("{\"nome\":\"Arroz\",\"idTipoArtigo\":\"abc\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
    }

    @Test
    void criarComIdDeTipoDecimalDa400() throws Exception {
        criar("{\"nome\":\"Arroz\",\"idTipoArtigo\":2.5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
    }

    @Test
    void jsonMalFormadoDa400() throws Exception {
        criar("{")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
    }

    @Test
    void criarComTipoInexistenteDa404() throws Exception {
        when(tipoArtigoRepository.findById(9)).thenReturn(Optional.empty());

        criar("{\"nome\":\"Arroz\",\"idTipoArtigo\":9}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("O tipo de artigo não existe."));
    }

    @Test
    void nomeDuplicadoNoMesmoTipoDa409() throws Exception {
        when(tipoArtigoRepository.findById(2)).thenReturn(Optional.of(mercearia));
        when(artigoRepository.findByIdTipoArtigoAndNomeIgnoreCase(2, "ARROZ"))
                .thenReturn(Optional.of(criarArtigo(1, "Arroz")));

        criar("{\"nome\":\"ARROZ\",\"idTipoArtigo\":2}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Já existe um artigo com este nome neste tipo de artigo."));
    }

    @Test
    void atualizarDevolve200ComOTipo() throws Exception {
        Artigo artigo = criarArtigo(1, "Arroz");
        when(artigoRepository.findById(1)).thenReturn(Optional.of(artigo));
        when(tipoArtigoRepository.findById(2)).thenReturn(Optional.of(mercearia));
        when(artigoRepository.findByIdTipoArtigoAndNomeIgnoreCase(2, "Massa")).thenReturn(Optional.empty());
        when(artigoRepository.saveAndFlush(artigo)).thenReturn(artigo);

        mockMvc.perform(put(BASE + "/1").header("Authorization", autorizacao)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"Massa\",\"idTipoArtigo\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Massa"))
                .andExpect(jsonPath("$.tipo.nome").value("Mercearia"));
    }

    @Test
    void apagarDevolve204() throws Exception {
        when(artigoRepository.findById(1)).thenReturn(Optional.of(criarArtigo(1, "Arroz")));

        mockMvc.perform(delete(BASE + "/1").header("Authorization", autorizacao))
                .andExpect(status().isNoContent());
    }

    @Test
    void apagarComItensAssociadosDa409() throws Exception {
        when(artigoRepository.findById(1)).thenReturn(Optional.of(criarArtigo(1, "Arroz")));
        doThrow(new DataIntegrityViolationException("fk")).when(artigoRepository).flush();

        mockMvc.perform(delete(BASE + "/1").header("Authorization", autorizacao))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Não é possível apagar um artigo com itens de compra associados."));
    }
}
