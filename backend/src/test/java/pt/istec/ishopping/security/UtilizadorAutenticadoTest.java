package pt.istec.ishopping.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import pt.istec.ishopping.exception.NaoAutenticadoException;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.UtilizadorRepository;

@ExtendWith(MockitoExtension.class)
class UtilizadorAutenticadoTest {

    @Mock
    private UtilizadorRepository utilizadorRepository;
    @InjectMocks
    private UtilizadorAutenticado utilizadorAutenticado;

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComSubject(String subject) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256").subject(subject).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void devolveOUtilizadorDoSubjectDoToken() {
        Utilizador utilizador = new Utilizador();
        autenticarComSubject("7");
        when(utilizadorRepository.findById(7)).thenReturn(Optional.of(utilizador));

        assertThat(utilizadorAutenticado.obter()).isSameAs(utilizador);
    }

    @Test
    void semAutenticacaoLancaExcecao() {
        assertThatThrownBy(() -> utilizadorAutenticado.obter()).isInstanceOf(NaoAutenticadoException.class);
    }

    @Test
    void utilizadorInexistenteLancaExcecao() {
        autenticarComSubject("7");
        when(utilizadorRepository.findById(7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utilizadorAutenticado.obter()).isInstanceOf(NaoAutenticadoException.class);
    }

    @Test
    void subjectNaoNumericoLancaExcecao() {
        autenticarComSubject("abc");

        assertThatThrownBy(() -> utilizadorAutenticado.obter()).isInstanceOf(NaoAutenticadoException.class);
    }
}
