package pt.istec.ishopping.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record EstatisticaCompraFechadaResposta(
        Integer id,
        String nome,
        Instant dataCriacao,
        Instant dataFechada,
        BigDecimal totalGasto,
        long totalItens,
        long itensPrevistos,
        long itensNaoPrevistos,
        int percentagemPrevistos,
        int percentagemNaoPrevistos) {
}
