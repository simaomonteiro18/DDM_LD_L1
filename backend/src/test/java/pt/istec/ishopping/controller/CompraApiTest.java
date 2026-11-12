package pt.istec.ishopping.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;

import pt.istec.ishopping.config.InicializadorUtilizador;
import pt.istec.ishopping.model.Compra;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.CompraRepository;
import pt.istec.ishopping.repository.UtilizadorRepository;
import pt.istec.ishopping.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompraApiTest {

    private static final String BASE = "/api/compras";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private CompraRepository compraRepository;
    @MockitoBean
    private UtilizadorRepository utilizadorRepository;
    @MockitoBean
    private PlatformTransactionManager transactionManager;
    @MockitoBean
    private InicializadorUtilizador inicializadorUtilizador;

    private String autorizacao;
    private Utilizador autenticado;

    @BeforeEach
    void preparar() {
        autenticado = new Utilizador();
        ReflectionTestUtils.setField(autenticado, "id", 1);
        autenticado.setNome("Ana");
        autenticado.setUsername("ana");
        when(utilizadorRepository.findById(1)).thenReturn(Optional.of(autenticado));
        autorizacao = "Bearer " + jwtService.gerar(autenticado).valor();
    }

    private Compra criarCompra(Integer id, String nome) {
        Compra compra = new Compra();
        ReflectionTestUtils.setField(compra, "id", id);
        compra.setNome(nome);
        compra.setCriadoPor(autenticado);
        return compra;
    }

    private Compra criarCompraFechada(Integer id) {
        Compra compra = criarCompra(id, "Semana 1");
        compra.fechar(autenticado);
        return compra;
    }

    private ResultActions criar(String json) throws Exception {
        return mockMvc.perform(post(BASE).header("Authorization", autorizacao)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions atualizar(Integer id, String json) throws Exception {
        return mockMvc.perform(put(BASE + "/" + id).header("Authorization", autorizacao)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions fechar(Integer id) throws Exception {
        return mockMvc.perform(post(BASE + "/" + id + "/fechar").header("Authorization", autorizacao));
    }

    @Test
    void pedidosSemTokenDao401() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + "/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(BASE + "/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(BASE + "/1/fechar")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void listarDevolveAEstruturaEsperada() throws Exception {
        when(compraRepository.findAllByOrderByDataCriacaoDescIdDesc())
                .thenReturn(List.of(criarCompra(2, "Semana 2"), criarCompraFechada(1)));

        mockMvc.perform(get(BASE).header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].nome").value("Semana 2"))
                .andExpect(jsonPath("$[0].fechada").value(false))
                .andExpect(jsonPath("$[0].criadoPor.username").value("ana"))
                .andExpect(jsonPath("$[0].fechadaPor").isEmpty())
                .andExpect(jsonPath("$[1].fechada").value(true))
                .andExpect(jsonPath("$[1].fechadaPor.username").value("ana"))
                .andExpect(jsonPath("$[1].dataFechada").isNotEmpty());
    }

    @Test
    void listarComFiltroUsaOEstado() throws Exception {
        when(compraRepository.findByFechadaOrderByDataCriacaoDescIdDesc(false))
                .thenReturn(List.of(criarCompra(2, "Semana 2")));

        mockMvc.perform(get(BASE).param("fechada", "false").header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fechada").value(false));
    }

    @Test
    void filtroInvalidoDa400() throws Exception {
        mockMvc.perform(get(BASE).param("fechada", "talvez").header("Authorization", autorizacao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
    }

    @Test
    void obterDevolve200() throws Exception {
        when(compraRepository.findById(1)).thenReturn(Optional.of(criarCompra(1, "Semana 1")));

        mockMvc.perform(get(BASE + "/1").header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Semana 1"));
    }

    @Test
    void obterInexistenteDa404NoFormatoDeErro() throws Exception {
        when(compraRepository.findById(9)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/9").header("Authorization", autorizacao))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.erro").value("A compra não existe."))
                .andExpect(jsonPath("$.caminho").value(BASE + "/9"));
    }

    @Test
    void idQueNaoEhNumeroDa400() throws Exception {
        mockMvc.perform(get(BASE + "/abc").header("Authorization", autorizacao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));
    }

    @Test
    void criarDevolve201ComLocationEOCriador() throws Exception {
        when(compraRepository.saveAndFlush(any(Compra.class))).thenAnswer(pedido -> {
            Compra guardada = pedido.getArgument(0);
            ReflectionTestUtils.setField(guardada, "id", 4);
            return guardada;
        });

        criar("{\"nome\":\"  Semana 1 \"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/4")))
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.nome").value("Semana 1"))
                .andExpect(jsonPath("$.fechada").value(false))
                .andExpect(jsonPath("$.criadoPor.username").value("ana"));
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
    void atualizarDevolve200ComQuemAlterou() throws Exception {
        Compra compra = criarCompra(1, "Semana 1");
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(compraRepository.saveAndFlush(compra)).thenReturn(compra);

        atualizar(1, "{\"nome\":\"Semana 2\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Semana 2"))
                .andExpect(jsonPath("$.alteradoPor.username").value("ana"))
                .andExpect(jsonPath("$.dataAlterado").isNotEmpty());
    }

    @Test
    void atualizarCompraFechadaDa409() throws Exception {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(criarCompraFechada(1)));

        atualizar(1, "{\"nome\":\"Semana 2\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.erro").value("Não é possível alterar uma compra fechada."));
    }

    @Test
    void atualizarInexistenteDa404() throws Exception {
        when(compraRepository.findByIdParaAlterar(9)).thenReturn(Optional.empty());

        atualizar(9, "{\"nome\":\"Semana 2\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("A compra não existe."));
    }

    @Test
    void atualizarComDadosInvalidosDa400() throws Exception {
        atualizar(1, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O nome é obrigatório."));
    }

    @Test
    void fecharDevolve200ComQuemFechou() throws Exception {
        Compra compra = criarCompra(1, "Semana 1");
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(compraRepository.saveAndFlush(compra)).thenReturn(compra);

        fechar(1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fechada").value(true))
                .andExpect(jsonPath("$.fechadaPor.username").value("ana"))
                .andExpect(jsonPath("$.dataFechada").isNotEmpty());
    }

    @Test
    void fecharDuasVezesDa409() throws Exception {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(criarCompraFechada(1)));

        fechar(1)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("A compra já está fechada."));
    }

    @Test
    void fecharInexistenteDa404() throws Exception {
        when(compraRepository.findByIdParaAlterar(9)).thenReturn(Optional.empty());

        fechar(9)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("A compra não existe."));
    }

    @Test
    void apagarCompraAbertaDevolve204() throws Exception {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(criarCompra(1, "Semana 1")));

        mockMvc.perform(delete(BASE + "/1").header("Authorization", autorizacao))
                .andExpect(status().isNoContent());
    }

    @Test
    void apagarCompraFechadaDa409() throws Exception {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(criarCompraFechada(1)));

        mockMvc.perform(delete(BASE + "/1").header("Authorization", autorizacao))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Não é possível apagar uma compra fechada."));
    }

    @Test
    void apagarInexistenteDa404() throws Exception {
        when(compraRepository.findByIdParaAlterar(9)).thenReturn(Optional.empty());

        mockMvc.perform(delete(BASE + "/9").header("Authorization", autorizacao))
                .andExpect(status().isNotFound());
    }
}
