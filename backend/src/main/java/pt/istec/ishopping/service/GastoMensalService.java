package pt.istec.ishopping.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import pt.istec.ishopping.repository.GastoCompra;
import pt.istec.ishopping.repository.ItemCompraRepository;

// Regra unica do gasto de um mes: soma de quantidade adquirida x preco unitario dos itens
// das compras criadas nesse mes (abertas e fechadas), contando o mes no fuso de Lisboa
@Service
public class GastoMensalService {

    private static final ZoneId ZONA = ZoneId.of("Europe/Lisbon");

    private final ItemCompraRepository itemCompraRepository;
    private final Clock relogio;

    public GastoMensalService(ItemCompraRepository itemCompraRepository, Clock relogio) {
        this.itemCompraRepository = itemCompraRepository;
        this.relogio = relogio;
    }

    public LocalDate hoje() {
        return LocalDate.ofInstant(relogio.instant(), ZONA);
    }

    public BigDecimal doMes(YearMonth mes) {
        BigDecimal somado = itemCompraRepository.somarGastoEntre(inicio(mes), inicio(mes.plusMonths(1)));
        return arredondar(somado == null ? BigDecimal.ZERO : somado);
    }

    // Gasto de cada um dos 12 meses do ano, com uma so consulta (meses sem compras ficam a zero)
    public Map<Integer, BigDecimal> porMesDoAno(int ano) {
        Map<Integer, BigDecimal> gastos = new HashMap<>();
        for (int mes = 1; mes <= 12; mes++) {
            gastos.put(mes, BigDecimal.ZERO);
        }
        YearMonth janeiro = YearMonth.of(ano, 1);
        for (GastoCompra compra : itemCompraRepository.somarGastoPorCompraEntre(inicio(janeiro),
                inicio(janeiro.plusYears(1)))) {
            if (compra.total() != null) {
                int mes = LocalDate.ofInstant(compra.dataCriacao(), ZONA).getMonthValue();
                gastos.merge(mes, compra.total(), BigDecimal::add);
            }
        }
        gastos.replaceAll((mes, gasto) -> arredondar(gasto));
        return gastos;
    }

    private static Instant inicio(YearMonth mes) {
        return mes.atDay(1).atStartOfDay(ZONA).toInstant();
    }

    private static BigDecimal arredondar(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }
}
