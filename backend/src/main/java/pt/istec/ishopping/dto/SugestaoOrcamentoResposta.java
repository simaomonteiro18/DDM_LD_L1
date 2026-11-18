package pt.istec.ishopping.dto;

import java.math.BigDecimal;

public record SugestaoOrcamentoResposta(Integer ano, Integer mes, BigDecimal valorSugerido, int baseadoEm) {
}
