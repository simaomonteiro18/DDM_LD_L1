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

import java.math.BigDecimal;
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
import pt.istec.ishopping.model.Orcamento;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.OrcamentoRepository;
import pt.istec.ishopping.repository.UtilizadorRepository;
import pt.istec.ishopping.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrcamentoApiTest {

    private static final String BASE = "/api/orcamentos";
    private static final String PERIODO_EM_USO = "Já existe um orçamento para este mês e ano.";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private OrcamentoRepository orcamentoRepository;
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
        when(utilizadorRepository.existsById(1)).thenReturn(true);
        autorizacao = "Bearer " + jwtService.gerar(autenticado).valor();
    }

    private Orcamento criarOrcamento(Integer id, int ano, int mes, String valor) {
        Orcamento orcamento = new Orcamento();
        ReflectionTestUtils.setField(orcamento, "id", id);
        orcamento.setAno(ano);
        orcamento.setMes(mes);
        orcamento.setValor(new BigDecimal(valor));
        orcamento.setCriadoPor(autenticado);
        return orcamento;
    }

    private ResultActions criar(String json) throws Exception {
        return mockMvc.perform(post(BASE).header("Authorization", autorizacao)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions atualizar(Integer id, String json) throws Exception {
        return mockMvc.perform(put(BASE + "/" + id).header("Authorization", autorizacao)
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
        when(orcamentoRepository.findAllComUtilizadores()).thenReturn(List.of(criarOrcamento(1, 2026, 5, "300.50")));

        mockMvc.perform(get(BASE).header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].ano").value(2026))
                .andExpect(jsonPath("$[0].mes").value(5))
                .andExpect(jsonPath("$[0].valor").value(300.50))
                .andExpect(jsonPath("$[0].criadoPor.username").value("ana"))
                .andExpect(jsonPath("$[0].alteradoPor").isEmpty());
    }

    @Test
    void listarComFiltroUsaOAno() throws Exception {
        when(orcamentoRepository.findAllComUtilizadoresPorAno(2025))
                .thenReturn(List.of(criarOrcamento(3, 2025, 12, "100.00")));

        mockMvc.perform(get(BASE).param("ano", "2025").header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ano").value(2025));
    }

    @Test
    void filtroQueNaoEhNumeroDa400() throws Exception {
        mockMvc.perform(get(BASE).param("ano", "abc").header("Authorization", autorizacao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
    }

    @Test
    void obterDevolve200() throws Exception {
        when(orcamentoRepository.findById(1)).thenReturn(Optional.of(criarOrcamento(1, 2026, 5, "300.00")));

        mockMvc.perform(get(BASE + "/1").header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.criadoPor.id").value(1));
    }

    @Test
    void obterInexistenteDa404NoFormatoDeErro() throws Exception {
        when(orcamentoRepository.findById(9)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/9").header("Authorization", autorizacao))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.erro").value("O orçamento não existe."))
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
        when(orcamentoRepository.findByAnoAndMes(2026, 5)).thenReturn(Optional.empty());
        when(orcamentoRepository.saveAndFlush(any(Orcamento.class))).thenAnswer(pedido -> {
            Orcamento guardado = pedido.getArgument(0);
            ReflectionTestUtils.setField(guardado, "id", 4);
            return guardado;
        });

        criar("{\"ano\":2026,\"mes\":5,\"valor\":300.5}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith(BASE + "/4")))
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.valor").value(300.50))
                .andExpect(jsonPath("$.criadoPor.username").value("ana"));
    }

    @Test
    void criarComDadosEmFaltaDa400ComMensagensEmPortugues() throws Exception {
        criar("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.erro").value(containsString("O ano é obrigatório.")))
                .andExpect(jsonPath("$.erro").value(containsString("O mês é obrigatório.")))
                .andExpect(jsonPath("$.erro").value(containsString("O valor é obrigatório.")));
    }

    @Test
    void criarComMesForaDoIntervaloDa400() throws Exception {
        criar("{\"ano\":2026,\"mes\":13,\"valor\":10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O mês tem de estar entre 1 e 12."));
        criar("{\"ano\":2026,\"mes\":0,\"valor\":10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O mês tem de estar entre 1 e 12."));
    }

    @Test
    void criarComAnoForaDoIntervaloDa400() throws Exception {
        criar("{\"ano\":1999,\"mes\":5,\"valor\":10}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O ano tem de estar entre 2000 e 2100."));
    }

    @Test
    void criarComValorNegativoDa400() throws Exception {
        criar("{\"ano\":2026,\"mes\":5,\"valor\":-0.01}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O valor não pode ser negativo."));
    }

    @Test
    void criarComValorForaDaPrecisaoDa400() throws Exception {
        criar("{\"ano\":2026,\"mes\":5,\"valor\":10.123}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("2 casas decimais")));
        criar("{\"ano\":2026,\"mes\":5,\"valor\":123456789}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value(containsString("8 algarismos inteiros")));
    }

    @Test
    void criarComValorQueNaoEhNumeroDa400() throws Exception {
        criar("{\"ano\":2026,\"mes\":5,\"valor\":\"abc\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
    }

    @Test
    void criarComMesDecimalDa400() throws Exception {
        criar("{\"ano\":2026,\"mes\":5.5,\"valor\":10}")
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
    void criarComPeriodoDuplicadoDa409() throws Exception {
        when(orcamentoRepository.findByAnoAndMes(2026, 5)).thenReturn(Optional.of(criarOrcamento(1, 2026, 5, "1")));

        criar("{\"ano\":2026,\"mes\":5,\"valor\":10}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.erro").value(PERIODO_EM_USO));
    }

    @Test
    void atualizarDevolve200ComQuemAlterou() throws Exception {
        Orcamento orcamento = criarOrcamento(1, 2026, 5, "300.00");
        when(orcamentoRepository.findById(1)).thenReturn(Optional.of(orcamento));
        when(orcamentoRepository.findByAnoAndMes(2026, 6)).thenReturn(Optional.empty());
        when(orcamentoRepository.saveAndFlush(orcamento)).thenReturn(orcamento);

        atualizar(1, "{\"ano\":2026,\"mes\":6,\"valor\":450}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value(6))
                .andExpect(jsonPath("$.valor").value(450.00))
                .andExpect(jsonPath("$.alteradoPor.username").value("ana"))
                .andExpect(jsonPath("$.dataAlterado").isNotEmpty());
    }

    @Test
    void atualizarParaPeriodoDuplicadoDa409() throws Exception {
        when(orcamentoRepository.findById(1)).thenReturn(Optional.of(criarOrcamento(1, 2026, 5, "300.00")));
        when(orcamentoRepository.findByAnoAndMes(2026, 6)).thenReturn(Optional.of(criarOrcamento(2, 2026, 6, "1")));

        atualizar(1, "{\"ano\":2026,\"mes\":6,\"valor\":450}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value(PERIODO_EM_USO));
    }

    @Test
    void atualizarInexistenteDa404() throws Exception {
        when(orcamentoRepository.findById(9)).thenReturn(Optional.empty());

        atualizar(9, "{\"ano\":2026,\"mes\":6,\"valor\":450}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").value("O orçamento não existe."));
    }

    @Test
    void atualizarComDadosInvalidosDa400() throws Exception {
        atualizar(1, "{\"ano\":2026,\"mes\":6,\"valor\":-5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O valor não pode ser negativo."));
    }

    @Test
    void apagarDevolve204() throws Exception {
        when(orcamentoRepository.findById(1)).thenReturn(Optional.of(criarOrcamento(1, 2026, 5, "300.00")));

        mockMvc.perform(delete(BASE + "/1").header("Authorization", autorizacao))
                .andExpect(status().isNoContent());
    }

    @Test
    void apagarInexistenteDa404() throws Exception {
        when(orcamentoRepository.findById(9)).thenReturn(Optional.empty());

        mockMvc.perform(delete(BASE + "/9").header("Authorization", autorizacao))
                .andExpect(status().isNotFound());
    }

    @Test
    void apagarComRegistosAssociadosDa409() throws Exception {
        when(orcamentoRepository.findById(1)).thenReturn(Optional.of(criarOrcamento(1, 2026, 5, "300.00")));
        doThrow(new DataIntegrityViolationException("fk")).when(orcamentoRepository).flush();

        mockMvc.perform(delete(BASE + "/1").header("Authorization", autorizacao))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erro").value("Não é possível apagar um orçamento com registos associados."));
    }
}
