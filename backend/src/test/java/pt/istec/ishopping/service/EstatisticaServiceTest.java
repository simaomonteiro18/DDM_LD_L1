package pt.istec.ishopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pt.istec.ishopping.dto.EstatisticaCompraFechadaResposta;
import pt.istec.ishopping.dto.EstatisticaMesResposta;
import pt.istec.ishopping.exception.PedidoInvalidoException;
import pt.istec.ishopping.model.Compra;
import pt.istec.ishopping.model.Orcamento;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.CompraRepository;
import pt.istec.ishopping.repository.GastoCompra;
import pt.istec.ishopping.repository.ItemCompraRepository;
import pt.istec.ishopping.repository.OrcamentoRepository;
import pt.istec.ishopping.repository.ResumoItensCompra;

@ExtendWith(MockitoExtension.class)
class EstatisticaServiceTest {

    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);
    private static final Instant INICIO_2026 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant INICIO_2027 = Instant.parse("2027-01-01T00:00:00Z");

    @Mock
    private OrcamentoRepository orcamentoRepository;
    @Mock
    private CompraRepository compraRepository;
    @Mock
    private ItemCompraRepository itemCompraRepository;

    private EstatisticaService estatisticaService;

    @BeforeEach
    void preparar() {
        estatisticaService = new EstatisticaService(orcamentoRepository, compraRepository, itemCompraRepository,
                new GastoMensalService(itemCompraRepository, RELOGIO));
    }

    private Orcamento criarOrcamento(int mes, String valor) {
        Orcamento orcamento = new Orcamento();
        orcamento.setAno(2026);
        orcamento.setMes(mes);
        orcamento.setValor(new BigDecimal(valor));
        return orcamento;
    }

    private Compra criarCompraFechada(Integer id, String nome) {
        Compra compra = new Compra();
        ReflectionTestUtils.setField(compra, "id", id);
        ReflectionTestUtils.setField(compra, "dataCriacao", Instant.parse("2026-06-01T10:00:00Z"));
        compra.setNome(nome);
        compra.fechar(new Utilizador());
        return compra;
    }

    @Test
    void mesesDeUmAnoSemDadosTemOsDozeMesesVazios() {
        when(orcamentoRepository.findByAno(2026)).thenReturn(List.of());
        when(itemCompraRepository.somarGastoPorCompraEntre(INICIO_2026, INICIO_2027)).thenReturn(List.of());

        List<EstatisticaMesResposta> meses = estatisticaService.meses(2026);

        assertThat(meses).extracting(EstatisticaMesResposta::mes)
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        assertThat(meses).allSatisfy(mes -> {
            assertThat(mes.orcamento()).isNull();
            assertThat(mes.totalCompras()).isEqualTo(new BigDecimal("0.00"));
            assertThat(mes.diferenca()).isNull();
        });
    }

    @Test
    void mesesComOrcamentoEComprasCalculaADiferenca() {
        when(orcamentoRepository.findByAno(2026)).thenReturn(List.of(criarOrcamento(1, "100"), criarOrcamento(2, "50")));
        when(itemCompraRepository.somarGastoPorCompraEntre(INICIO_2026, INICIO_2027)).thenReturn(List.of(
                new GastoCompra(1, Instant.parse("2026-01-10T10:00:00Z"), new BigDecimal("30.00")),
                new GastoCompra(2, Instant.parse("2026-02-10T10:00:00Z"), new BigDecimal("75.50")),
                new GastoCompra(3, Instant.parse("2026-03-10T10:00:00Z"), new BigDecimal("12.00"))));

        List<EstatisticaMesResposta> meses = estatisticaService.meses(2026);

        assertThat(meses.get(0)).isEqualTo(new EstatisticaMesResposta(1, new BigDecimal("100.00"),
                new BigDecimal("30.00"), new BigDecimal("70.00")));
        assertThat(meses.get(1)).isEqualTo(new EstatisticaMesResposta(2, new BigDecimal("50.00"),
                new BigDecimal("75.50"), new BigDecimal("-25.50")));
        assertThat(meses.get(2)).isEqualTo(new EstatisticaMesResposta(3, null, new BigDecimal("12.00"), null));
    }

    @Test
    void mesesSemAnoUsaOAnoAtualELimitaAsComprasAEsseAno() {
        when(orcamentoRepository.findByAno(2026)).thenReturn(List.of());
        when(itemCompraRepository.somarGastoPorCompraEntre(any(), any())).thenReturn(List.of());

        estatisticaService.meses(null);

        verify(orcamentoRepository).findByAno(2026);
        verify(itemCompraRepository).somarGastoPorCompraEntre(INICIO_2026, INICIO_2027);
    }

    @Test
    void mesesComAnoForaDoIntervaloDaPedidoInvalido() {
        assertThatThrownBy(() -> estatisticaService.meses(1999))
                .isInstanceOf(PedidoInvalidoException.class)
                .hasMessage("O ano tem de estar entre 2000 e 2100.");
        assertThatThrownBy(() -> estatisticaService.meses(2101))
                .isInstanceOf(PedidoInvalidoException.class);
        verify(orcamentoRepository, never()).findByAno(any());
    }

    @Test
    void comprasFechadasResumemOsItensComUmaSoConsulta() {
        Compra comItens = criarCompraFechada(2, "Semana 2");
        Compra semItens = criarCompraFechada(1, "Semana 1");
        when(compraRepository.findByFechadaTrueOrderByDataFechadaDescIdDesc()).thenReturn(List.of(comItens, semItens));
        when(itemCompraRepository.resumirPorCompra(List.of(2, 1)))
                .thenReturn(List.of(new ResumoItensCompra(2, 3L, 1L, 2L, new BigDecimal("12.3"))));

        List<EstatisticaCompraFechadaResposta> compras = estatisticaService.comprasFechadas();

        assertThat(compras).containsExactly(
                new EstatisticaCompraFechadaResposta(2, "Semana 2", comItens.getDataCriacao(), comItens.getDataFechada(),
                        new BigDecimal("12.30"), 3, 1, 2, 33, 67),
                new EstatisticaCompraFechadaResposta(1, "Semana 1", semItens.getDataCriacao(), semItens.getDataFechada(),
                        new BigDecimal("0.00"), 0, 0, 0, 0, 0));
    }

    @Test
    void comprasFechadasComItensPorAdquirirTemGastoZero() {
        when(compraRepository.findByFechadaTrueOrderByDataFechadaDescIdDesc())
                .thenReturn(List.of(criarCompraFechada(1, "Semana 1")));
        when(itemCompraRepository.resumirPorCompra(List.of(1)))
                .thenReturn(List.of(new ResumoItensCompra(1, 2L, 2L, 0L, null)));

        EstatisticaCompraFechadaResposta compra = estatisticaService.comprasFechadas().get(0);

        assertThat(compra.totalGasto()).isEqualTo(new BigDecimal("0.00"));
        assertThat(compra.percentagemPrevistos()).isEqualTo(100);
        assertThat(compra.percentagemNaoPrevistos()).isZero();
    }

    @Test
    void percentagensDasComprasFechadasSomamSempreCem() {
        when(compraRepository.findByFechadaTrueOrderByDataFechadaDescIdDesc())
                .thenReturn(List.of(criarCompraFechada(1, "Semana 1")));
        when(itemCompraRepository.resumirPorCompra(List.of(1)))
                .thenReturn(List.of(new ResumoItensCompra(1, 8L, 1L, 7L, BigDecimal.TEN)));

        EstatisticaCompraFechadaResposta compra = estatisticaService.comprasFechadas().get(0);

        // 1 em 8 sao 12,5 % (arredonda para 13) e os nao previstos ficam com o resto
        assertThat(compra.percentagemPrevistos()).isEqualTo(13);
        assertThat(compra.percentagemNaoPrevistos()).isEqualTo(87);
    }

    @Test
    void semComprasFechadasNaoResumeItens() {
        when(compraRepository.findByFechadaTrueOrderByDataFechadaDescIdDesc()).thenReturn(List.of());

        assertThat(estatisticaService.comprasFechadas()).isEmpty();
        verify(itemCompraRepository, never()).resumirPorCompra(any());
    }
}
