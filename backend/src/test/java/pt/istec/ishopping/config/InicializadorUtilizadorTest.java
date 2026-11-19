package pt.istec.ishopping.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.UtilizadorRepository;

@ExtendWith(MockitoExtension.class)
class InicializadorUtilizadorTest {

    private static final AdminProperties ADMIN = new AdminProperties("admin", "Administrador", "segredo");

    @Mock
    private UtilizadorRepository utilizadorRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void tabelaVaziaComVariaveisCriaUtilizadorComHash() {
        when(utilizadorRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("segredo")).thenReturn("hash");

        new InicializadorUtilizador(utilizadorRepository, passwordEncoder, ADMIN).run(null);

        ArgumentCaptor<Utilizador> criado = ArgumentCaptor.forClass(Utilizador.class);
        verify(utilizadorRepository).save(criado.capture());
        assertThat(criado.getValue().getUsername()).isEqualTo("admin");
        assertThat(criado.getValue().getNome()).isEqualTo("Administrador");
        assertThat(criado.getValue().getPasswordHash()).isEqualTo("hash");
    }

    @Test
    void tabelaVaziaSemVariaveisNaoCriaNada() {
        when(utilizadorRepository.count()).thenReturn(0L);

        new InicializadorUtilizador(utilizadorRepository, passwordEncoder, new AdminProperties("admin", "", null))
                .run(null);

        verify(utilizadorRepository, never()).save(any());
    }

    @Test
    void comUtilizadoresExistentesNaoFazNada() {
        when(utilizadorRepository.count()).thenReturn(1L);

        new InicializadorUtilizador(utilizadorRepository, passwordEncoder, ADMIN).run(null);

        verify(utilizadorRepository, never()).save(any());
    }
}
