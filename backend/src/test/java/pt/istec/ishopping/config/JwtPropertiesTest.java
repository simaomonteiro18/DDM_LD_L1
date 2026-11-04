package pt.istec.ishopping.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class JwtPropertiesTest {

    @EnableConfigurationProperties(JwtProperties.class)
    static class Configuracao {
    }

    private final ApplicationContextRunner contexto = new ApplicationContextRunner()
            .withUserConfiguration(Configuracao.class)
            .withPropertyValues("ishopping.jwt.expiracao-horas=8");

    @Test
    void segredoEmFaltaImpedeOArranque() {
        contexto.withPropertyValues("ishopping.jwt.segredo=").run(arranque -> assertThat(arranque)
                .getFailure().rootCause().hasMessageContaining("JWT_SECRET é obrigatória"));
    }

    @Test
    void segredoCurtoImpedeOArranqueSemMostrarOValor() {
        contexto.withPropertyValues("ishopping.jwt.segredo=curto-demais").run(arranque -> assertThat(arranque)
                .getFailure().rootCause()
                .hasMessageContaining("pelo menos 32 caracteres")
                .hasMessageNotContaining("curto-demais"));
    }

    @Test
    void segredoCom32CaracteresEhAceite() {
        contexto.withPropertyValues("ishopping.jwt.segredo=12345678901234567890123456789012")
                .run(arranque -> assertThat(arranque).hasSingleBean(JwtProperties.class));
    }
}
