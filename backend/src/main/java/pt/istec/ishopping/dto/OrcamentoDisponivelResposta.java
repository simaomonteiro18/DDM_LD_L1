package pt.istec.ishopping.dto;

import java.math.BigDecimal;

public record OrcamentoDisponivelResposta(
        Integer ano,
        Integer mes,
        BigDecimal orcamento,
        BigDecimal gasto,
        BigDecimal disponivel,
        BigDecimal percentagemRestante,
        boolean ultrapassado) {
}
