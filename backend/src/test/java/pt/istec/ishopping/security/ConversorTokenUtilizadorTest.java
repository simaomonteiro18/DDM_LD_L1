package pt.istec.ishopping.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import pt.istec.ishopping.repository.UtilizadorRepository;

@ExtendWith(MockitoExtension.class)
class ConversorTokenUtilizadorTest {

    @Mock
    private UtilizadorRepository utilizadorRepository;
    @InjectMocks
    private ConversorTokenUtilizador conversor;

    private Jwt tokenComSubject(String subject) {
        return Jwt.withTokenValue("token").header("alg", "HS256").subject(subject).build();
    }

    @Test
    void utilizadorExistenteDevolveAAutenticacaoDoToken() {
        Jwt jwt = tokenComSubject("7");
        when(utilizadorRepository.existsById(7)).thenReturn(true);

        AbstractAuthenticationToken autenticacao = conversor.convert(jwt);

        assertThat(autenticacao).isInstanceOf(JwtAuthenticationToken.class);
        assertThat(autenticacao.getName()).isEqualTo("7");
        assertThat(((JwtAuthenticationToken) autenticacao).getToken()).isSameAs(jwt);
    }

    @Test
    void utilizadorInexistenteRejeitaOToken() {
        when(utilizadorRepository.existsById(7)).thenReturn(false);

        assertThatThrownBy(() -> conversor.convert(tokenComSubject("7")))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    void subjectNaoNumericoRejeitaOTokenSemConsultarABaseDeDados() {
        assertThatThrownBy(() -> conversor.convert(tokenComSubject("abc")))
                .isInstanceOf(InvalidBearerTokenException.class);
        verify(utilizadorRepository, never()).existsById(any());
    }

    @Test
    void tokenSemSubjectRejeitaOToken() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").claim("outro", "valor").build();

        assertThatThrownBy(() -> conversor.convert(jwt)).isInstanceOf(InvalidBearerTokenException.class);
        verify(utilizadorRepository, never()).existsById(any());
    }
}
