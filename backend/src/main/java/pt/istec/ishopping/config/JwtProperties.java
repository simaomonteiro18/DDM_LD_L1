package pt.istec.ishopping.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("ishopping.jwt")
public record JwtProperties(String segredo, int expiracaoHoras) {

    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    public JwtProperties {
        if (segredo == null || segredo.isBlank()) {
            throw new IllegalArgumentException("A variável JWT_SECRET é obrigatória e não está definida.");
        }
        if (segredo.length() < TAMANHO_MINIMO_SEGREDO) {
            throw new IllegalArgumentException(
                    "A variável JWT_SECRET tem de ter pelo menos " + TAMANHO_MINIMO_SEGREDO + " caracteres.");
        }
        if (expiracaoHoras < 1) {
            throw new IllegalArgumentException("A variável JWT_EXPIRACAO_HORAS tem de ser pelo menos 1.");
        }
    }
}
