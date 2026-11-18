package pt.istec.ishopping.dto;

import java.math.BigDecimal;

public record EstatisticaMesResposta(Integer mes, BigDecimal orcamento, BigDecimal totalCompras, BigDecimal diferenca) {
}
