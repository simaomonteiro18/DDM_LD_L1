package pt.istec.ishopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import pt.istec.ishopping.dto.LoginPedido;
import pt.istec.ishopping.dto.LoginResposta;
import pt.istec.ishopping.exception.NaoAutenticadoException;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.UtilizadorRepository;
import pt.istec.ishopping.security.JwtService;

@ExtendWith(MockitoExtension.class)
class AutenticacaoServiceTest {

    @Mock
    private UtilizadorRepository utilizadorRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AutenticacaoService autenticacaoService;
    private Utilizador utilizador;

    @BeforeEach
    void preparar() {
        when(passwordEncoder.encode(anyString())).thenReturn("hash-ficticio");
        autenticacaoService = new AutenticacaoService(utilizadorRepository, passwordEncoder, jwtService);
        utilizador = new Utilizador();
        utilizador.setNome("Ana Silva");
        utilizador.setUsername("ana");
        utilizador.setPasswordHash("hash-real");
    }

    @Test
    void loginValidoDevolveTokenEUtilizador() {
        Instant expiraEm = Instant.parse("2026-01-01T08:00:00Z");
        when(utilizadorRepository.findByUsernameIgnoreCase("ANA")).thenReturn(Optional.of(utilizador));
        when(passwordEncoder.matches("segredo", "hash-real")).thenReturn(true);
        when(jwtService.gerar(utilizador)).thenReturn(new JwtService.TokenGerado("token-teste", expiraEm));

        LoginResposta resposta = autenticacaoService.login(new LoginPedido("ANA", "segredo"));

        assertThat(resposta.token()).isEqualTo("token-teste");
        assertThat(resposta.tipo()).isEqualTo("Bearer");
        assertThat(resposta.expiraEm()).isEqualTo(expiraEm);
        assertThat(resposta.utilizador().nome()).isEqualTo("Ana Silva");
        assertThat(resposta.utilizador().username()).isEqualTo("ana");
    }

    @Test
    void passwordErradaEhRejeitada() {
        when(utilizadorRepository.findByUsernameIgnoreCase("ana")).thenReturn(Optional.of(utilizador));
        when(passwordEncoder.matches("errada", "hash-real")).thenReturn(false);

        assertThatThrownBy(() -> autenticacaoService.login(new LoginPedido("ana", "errada")))
                .isInstanceOf(NaoAutenticadoException.class)
                .hasMessage("Credenciais inválidas.");
        verify(jwtService, never()).gerar(utilizador);
    }

    @Test
    void usernameInexistenteEhRejeitadoComAMesmaMensagemEComparaHash() {
        when(utilizadorRepository.findByUsernameIgnoreCase("ninguem")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> autenticacaoService.login(new LoginPedido("ninguem", "segredo")))
                .isInstanceOf(NaoAutenticadoException.class)
                .hasMessage("Credenciais inválidas.");
        verify(passwordEncoder).matches("segredo", "hash-ficticio");
    }
}
