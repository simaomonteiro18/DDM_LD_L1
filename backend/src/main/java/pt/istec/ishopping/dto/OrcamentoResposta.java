package pt.istec.ishopping.dto;

import java.math.BigDecimal;
import java.time.Instant;

import pt.istec.ishopping.model.Orcamento;

public record OrcamentoResposta(
        Integer id,
        Integer ano,
        Integer mes,
        BigDecimal valor,
        UtilizadorResposta criadoPor,
        Instant dataCriacao,
        UtilizadorResposta alteradoPor,
        Instant dataAlterado) {

    public static OrcamentoResposta de(Orcamento orcamento) {
        return new OrcamentoResposta(
                orcamento.getId(),
                orcamento.getAno(),
                orcamento.getMes(),
                orcamento.getValor(),
                UtilizadorResposta.de(orcamento.getCriadoPor()),
                orcamento.getDataCriacao(),
                orcamento.getAlteradoPor() == null ? null : UtilizadorResposta.de(orcamento.getAlteradoPor()),
                orcamento.getDataAlterado());
    }
}
