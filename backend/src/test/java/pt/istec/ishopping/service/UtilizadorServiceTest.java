package pt.istec.ishopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import pt.istec.ishopping.dto.UtilizadorAtualizarPedido;
import pt.istec.ishopping.dto.UtilizadorCriarPedido;
import pt.istec.ishopping.dto.UtilizadorResposta;
import pt.istec.ishopping.exception.ConflitoException;
import pt.istec.ishopping.exception.RecursoNaoEncontradoException;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.UtilizadorRepository;
import pt.istec.ishopping.security.UtilizadorAutenticado;

@ExtendWith(MockitoExtension.class)
class UtilizadorServiceTest {

    @Mock
    private UtilizadorRepository utilizadorRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UtilizadorAutenticado utilizadorAutenticado;
    @InjectMocks
    private UtilizadorService utilizadorService;

    private Utilizador criarUtilizador(Integer id, String username, String hash) {
        Utilizador utilizador = new Utilizador();
        ReflectionTestUtils.setField(utilizador, "id", id);
        utilizador.setNome("Nome de " + username);
        utilizador.setUsername(username);
        utilizador.setPasswordHash(hash);
        return utilizador;
    }

    private void guardarDevolveOMesmo() {
        when(utilizadorRepository.saveAndFlush(any(Utilizador.class))).thenAnswer(pedido -> pedido.getArgument(0));
    }

    @Test
    void criarGuardaHashEDadosSemEspacosNasPontas() {
        when(utilizadorRepository.findByUsernameIgnoreCase("ana")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("segredo")).thenReturn("hash");
        when(utilizadorRepository.saveAndFlush(any(Utilizador.class))).thenAnswer(pedido -> {
            Utilizador guardado = pedido.getArgument(0);
            assertThat(guardado.getPasswordHash()).isEqualTo("hash");
            ReflectionTestUtils.setField(guardado, "id", 5);
            return guardado;
        });

        UtilizadorResposta resposta = utilizadorService.criar(new UtilizadorCriarPedido("  Ana Silva ", " ana ", "segredo"));

        assertThat(resposta).isEqualTo(new UtilizadorResposta(5, "Ana Silva", "ana"));
    }

    @Test
    void criarComUsernameDuplicadoComOutrasMaiusculasDaConflito() {
        when(utilizadorRepository.findByUsernameIgnoreCase("ANA"))
                .thenReturn(Optional.of(criarUtilizador(1, "ana", "hash")));

        assertThatThrownBy(() -> utilizadorService.criar(new UtilizadorCriarPedido("Ana", "ANA", "segredo")))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Já existe um utilizador com este username.");
        verify(utilizadorRepository, never()).saveAndFlush(any());
    }

    @Test
    void criarQueViolaOIndiceUnicoDaConflito() {
        when(utilizadorRepository.findByUsernameIgnoreCase("ana")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("segredo")).thenReturn("hash");
        when(utilizadorRepository.saveAndFlush(any(Utilizador.class)))
                .thenThrow(new DataIntegrityViolationException("uq_utilizador_username"));

        assertThatThrownBy(() -> utilizadorService.criar(new UtilizadorCriarPedido("Ana", "ana", "segredo")))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Já existe um utilizador com este username.");
    }

    @Test
    void atualizarSemPasswordMantemOHash() {
        Utilizador utilizador = criarUtilizador(1, "ana", "hash-atual");
        when(utilizadorRepository.findById(1)).thenReturn(Optional.of(utilizador));
        when(utilizadorRepository.findByUsernameIgnoreCase("ana.nova")).thenReturn(Optional.empty());
        guardarDevolveOMesmo();

        UtilizadorResposta resposta = utilizadorService.atualizar(1, new UtilizadorAtualizarPedido("Ana", "ana.nova", ""));

        assertThat(resposta).isEqualTo(new UtilizadorResposta(1, "Ana", "ana.nova"));
        assertThat(utilizador.getPasswordHash()).isEqualTo("hash-atual");
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void atualizarComPasswordGuardaNovoHash() {
        Utilizador utilizador = criarUtilizador(1, "ana", "hash-atual");
        when(utilizadorRepository.findById(1)).thenReturn(Optional.of(utilizador));
        when(utilizadorRepository.findByUsernameIgnoreCase("ana")).thenReturn(Optional.of(utilizador));
        when(passwordEncoder.encode("nova-password")).thenReturn("hash-novo");
        guardarDevolveOMesmo();

        utilizadorService.atualizar(1, new UtilizadorAtualizarPedido("Ana", "ana", "nova-password"));

        assertThat(utilizador.getPasswordHash()).isEqualTo("hash-novo");
    }

    @Test
    void atualizarIgnoraOProprioNaVerificacaoDoUsername() {
        Utilizador utilizador = criarUtilizador(1, "ana", "hash");
        when(utilizadorRepository.findById(1)).thenReturn(Optional.of(utilizador));
        when(utilizadorRepository.findByUsernameIgnoreCase("ANA")).thenReturn(Optional.of(utilizador));
        guardarDevolveOMesmo();

        UtilizadorResposta resposta = utilizadorService.atualizar(1, new UtilizadorAtualizarPedido("Ana", "ANA", null));

        assertThat(resposta.username()).isEqualTo("ANA");
    }

    @Test
    void atualizarComUsernameDeOutroDaConflito() {
        when(utilizadorRepository.findById(1)).thenReturn(Optional.of(criarUtilizador(1, "ana", "hash")));
        when(utilizadorRepository.findByUsernameIgnoreCase("RUI"))
                .thenReturn(Optional.of(criarUtilizador(2, "rui", "hash")));

        assertThatThrownBy(() -> utilizadorService.atualizar(1, new UtilizadorAtualizarPedido("Ana", "RUI", null)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Já existe um utilizador com este username.");
        verify(utilizadorRepository, never()).saveAndFlush(any());
    }

    @Test
    void atualizarInexistenteDaNaoEncontrado() {
        when(utilizadorRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utilizadorService.atualizar(9, new UtilizadorAtualizarPedido("Ana", "ana", null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void apagarFazFlushDentroDoServico() {
        Utilizador alvo = criarUtilizador(2, "rui", "hash");
        when(utilizadorRepository.findById(2)).thenReturn(Optional.of(alvo));
        when(utilizadorAutenticado.obter()).thenReturn(criarUtilizador(1, "ana", "hash"));

        utilizadorService.apagar(2);

        verify(utilizadorRepository).delete(alvo);
        verify(utilizadorRepository).flush();
    }

    @Test
    void apagarOProprioUtilizadorDaConflito() {
        Utilizador proprio = criarUtilizador(1, "ana", "hash");
        when(utilizadorRepository.findById(1)).thenReturn(Optional.of(proprio));
        when(utilizadorAutenticado.obter()).thenReturn(proprio);

        assertThatThrownBy(() -> utilizadorService.apagar(1))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Não é possível apagar o próprio utilizador autenticado.");
        verify(utilizadorRepository, never()).delete(any(Utilizador.class));
    }

    @Test
    void apagarInexistenteDaNaoEncontrado() {
        when(utilizadorRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> utilizadorService.apagar(9))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("O utilizador não existe.");
    }

    @Test
    void apagarComRegistosAssociadosDaConflito() {
        when(utilizadorRepository.findById(2)).thenReturn(Optional.of(criarUtilizador(2, "rui", "hash")));
        when(utilizadorAutenticado.obter()).thenReturn(criarUtilizador(1, "ana", "hash"));
        doThrow(new DataIntegrityViolationException("fk")).when(utilizadorRepository).flush();

        assertThatThrownBy(() -> utilizadorService.apagar(2))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Não é possível apagar um utilizador com registos associados.");
    }
}
