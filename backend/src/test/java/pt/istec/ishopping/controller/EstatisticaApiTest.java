package pt.istec.ishopping.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import pt.istec.ishopping.config.InicializadorUtilizador;
import pt.istec.ishopping.model.Compra;
import pt.istec.ishopping.model.Orcamento;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.CompraRepository;
import pt.istec.ishopping.repository.GastoCompra;
import pt.istec.ishopping.repository.ItemCompraRepository;
import pt.istec.ishopping.repository.OrcamentoRepository;
import pt.istec.ishopping.repository.ResumoItensCompra;
import pt.istec.ishopping.repository.UtilizadorRepository;
import pt.istec.ishopping.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EstatisticaApiTest {

    private static final String MESES = "/api/estatisticas/meses";
    private static final String COMPRAS_FECHADAS = "/api/estatisticas/compras-fechadas";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtService jwtService;
    @MockitoBean
    private OrcamentoRepository orcamentoRepository;
    @MockitoBean
    private CompraRepository compraRepository;
    @MockitoBean
    private ItemCompraRepository itemCompraRepository;
    @MockitoBean
    private UtilizadorRepository utilizadorRepository;
    @MockitoBean
    private PlatformTransactionManager transactionManager;
    @MockitoBean
    private InicializadorUtilizador inicializadorUtilizador;

    private String autorizacao;

    @BeforeEach
    void preparar() {
        Utilizador utilizador = new Utilizador();
        ReflectionTestUtils.setField(utilizador, "id", 1);
        when(utilizadorRepository.existsById(1)).thenReturn(true);
        autorizacao = "Bearer " + jwtService.gerar(utilizador).valor();
    }

    @Test
    void pedidosSemTokenDao401() throws Exception {
        mockMvc.perform(get(MESES)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(COMPRAS_FECHADAS))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.estado").value(401));
    }

    @Test
    void mesesDevolveOsDozeMesesComOrcamentoGastoEDiferenca() throws Exception {
        Orcamento janeiro = new Orcamento();
        janeiro.setAno(2026);
        janeiro.setMes(1);
        janeiro.setValor(new BigDecimal("100"));
        when(orcamentoRepository.findByAno(2026)).thenReturn(List.of(janeiro));
        when(itemCompraRepository.somarGastoPorCompraEntre(any(), any())).thenReturn(List.of(
                new GastoCompra(1, Instant.parse("2026-01-10T10:00:00Z"), new BigDecimal("112.50"))));

        mockMvc.perform(get(MESES).param("ano", "2026").header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].mes").value(1))
                .andExpect(jsonPath("$[0].orcamento").value(100.00))
                .andExpect(jsonPath("$[0].totalCompras").value(112.50))
                .andExpect(jsonPath("$[0].diferenca").value(-12.50))
                .andExpect(jsonPath("$[1].mes").value(2))
                .andExpect(jsonPath("$[1].orcamento").isEmpty())
                .andExpect(jsonPath("$[1].totalCompras").value(0.00))
                .andExpect(jsonPath("$[1].diferenca").isEmpty())
                .andExpect(jsonPath("$[11].mes").value(12));
    }

    @Test
    void mesesComAnoInvalidoDa400() throws Exception {
        mockMvc.perform(get(MESES).param("ano", "1999").header("Authorization", autorizacao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.erro").value("O ano tem de estar entre 2000 e 2100."))
                .andExpect(jsonPath("$.caminho").value(MESES));
        mockMvc.perform(get(MESES).param("ano", "abc").header("Authorization", autorizacao))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("O pedido é inválido."));
    }

    @Test
    void comprasFechadasDevolveOResumoDeCadaCompra() throws Exception {
        Compra compra = new Compra();
        ReflectionTestUtils.setField(compra, "id", 7);
        ReflectionTestUtils.setField(compra, "dataCriacao", Instant.parse("2026-06-01T10:00:00Z"));
        compra.setNome("Semana 1");
        compra.fechar(new Utilizador());
        when(compraRepository.findByFechadaTrueOrderByDataFechadaDescIdDesc()).thenReturn(List.of(compra));
        when(itemCompraRepository.resumirPorCompra(List.of(7)))
                .thenReturn(List.of(new ResumoItensCompra(7, 4L, 3L, 1L, new BigDecimal("20.5"))));

        mockMvc.perform(get(COMPRAS_FECHADAS).header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].nome").value("Semana 1"))
                .andExpect(jsonPath("$[0].dataCriacao").value("2026-06-01T10:00:00Z"))
                .andExpect(jsonPath("$[0].dataFechada").isNotEmpty())
                .andExpect(jsonPath("$[0].totalGasto").value(20.50))
                .andExpect(jsonPath("$[0].totalItens").value(4))
                .andExpect(jsonPath("$[0].itensPrevistos").value(3))
                .andExpect(jsonPath("$[0].itensNaoPrevistos").value(1))
                .andExpect(jsonPath("$[0].percentagemPrevistos").value(75))
                .andExpect(jsonPath("$[0].percentagemNaoPrevistos").value(25));
    }

    @Test
    void semComprasFechadasDevolveListaVazia() throws Exception {
        when(compraRepository.findByFechadaTrueOrderByDataFechadaDescIdDesc()).thenReturn(List.of());

        mockMvc.perform(get(COMPRAS_FECHADAS).header("Authorization", autorizacao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
