package pt.istec.ishopping.dto;

import java.time.Instant;

import pt.istec.ishopping.model.Compra;
import pt.istec.ishopping.model.Utilizador;

public record CompraResposta(
        Integer id,
        String nome,
        boolean fechada,
        UtilizadorResposta criadoPor,
        Instant dataCriacao,
        UtilizadorResposta alteradoPor,
        Instant dataAlterado,
        UtilizadorResposta fechadaPor,
        Instant dataFechada) {

    public static CompraResposta de(Compra compra) {
        return new CompraResposta(
                compra.getId(),
                compra.getNome(),
                compra.isFechada(),
                utilizador(compra.getCriadoPor()),
                compra.getDataCriacao(),
                utilizador(compra.getAlteradoPor()),
                compra.getDataAlterado(),
                utilizador(compra.getFechadaPor()),
                compra.getDataFechada());
    }

    private static UtilizadorResposta utilizador(Utilizador utilizador) {
        return utilizador == null ? null : UtilizadorResposta.de(utilizador);
    }
}
