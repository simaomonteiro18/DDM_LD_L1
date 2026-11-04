package pt.istec.ishopping.dto;

import java.time.Instant;

public record ErroResposta(Instant timestamp, int estado, String erro, String caminho) {

    public static ErroResposta de(int estado, String erro, String caminho) {
        return new ErroResposta(Instant.now(), estado, erro, caminho);
    }
}
