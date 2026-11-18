package pt.istec.ishopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.istec.ishopping.repository.GastoCompra;
import pt.istec.ishopping.repository.ItemCompraRepository;

@ExtendWith(MockitoExtension.class)
class GastoMensalServiceTest {

    // Em Lisboa ja e 1 de abril de 2026 (UTC+1 desde o fim de marco)
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-03-31T23:30:00Z"), ZoneOffset.UTC);
    private static final Instant INICIO_2026 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant INICIO_2027 = Instant.parse("2027-01-01T00:00:00Z");

    @Mock
    private ItemCompraRepository itemCompraRepository;

    private GastoMensalService gastoMensal;

    @BeforeEach
    void preparar() {
        gastoMensal = new GastoMensalService(itemCompraRepository, RELOGIO);
    }

    @Test
    void hojeEOdiaAtualEmLisboa() {
        assertThat(gastoMensal.hoje()).isEqualTo(LocalDate.of(2026, 4, 1));
    }

    @Test
    void doMesSemComprasEZero() {
        when(itemCompraRepository.somarGastoEntre(Instant.parse("2026-03-31T23:00:00Z"),
                Instant.parse("2026-04-30T23:00:00Z"))).thenReturn(null);

        assertThat(gastoMensal.doMes(YearMonth.of(2026, 4))).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void porMesDoAnoSemComprasTemOsDozeMesesAZero() {
        when(itemCompraRepository.somarGastoPorCompraEntre(INICIO_2026, INICIO_2027)).thenReturn(List.of());

        Map<Integer, BigDecimal> gastos = gastoMensal.porMesDoAno(2026);

        assertThat(gastos).hasSize(12);
        assertThat(gastos.values()).containsOnly(new BigDecimal("0.00"));
    }

    @Test
    void porMesDoAnoSomaAsComprasDeCadaMesNoFusoDeLisboa() {
        when(itemCompraRepository.somarGastoPorCompraEntre(INICIO_2026, INICIO_2027)).thenReturn(List.of(
                new GastoCompra(1, Instant.parse("2026-01-15T10:00:00Z"), new BigDecimal("5.50")),
                new GastoCompra(2, Instant.parse("2026-01-20T10:00:00Z"), new BigDecimal("2.25")),
                // 23:30 UTC de 31 de marco ja e 1 de abril em Lisboa
                new GastoCompra(3, Instant.parse("2026-03-31T23:30:00Z"), new BigDecimal("10")),
                // Compra so com itens por adquirir
                new GastoCompra(4, Instant.parse("2026-05-02T10:00:00Z"), null)));

        Map<Integer, BigDecimal> gastos = gastoMensal.porMesDoAno(2026);

        assertThat(gastos.get(1)).isEqualTo(new BigDecimal("7.75"));
        assertThat(gastos.get(3)).isEqualTo(new BigDecimal("0.00"));
        assertThat(gastos.get(4)).isEqualTo(new BigDecimal("10.00"));
        assertThat(gastos.get(5)).isEqualTo(new BigDecimal("0.00"));
    }
}
