package pt.istec.ishopping.dto;

import java.time.Instant;

public record LoginResposta(String token, String tipo, Instant expiraEm, UtilizadorResposta utilizador) {
}
