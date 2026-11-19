package pt.istec.ishopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.test.util.ReflectionTestUtils;

import pt.istec.ishopping.dto.TipoArtigoAtualizarPedido;
import pt.istec.ishopping.dto.TipoArtigoCriarPedido;
import pt.istec.ishopping.dto.TipoArtigoResposta;
import pt.istec.ishopping.exception.ConflitoException;
import pt.istec.ishopping.exception.RecursoNaoEncontradoException;
import pt.istec.ishopping.model.TipoArtigo;
import pt.istec.ishopping.repository.TipoArtigoRepository;

@ExtendWith(MockitoExtension.class)
class TipoArtigoServiceTest {

    private static final String NOME_EM_USO = "Já existe um tipo de artigo com este nome.";

    @Mock
    private TipoArtigoRepository tipoArtigoRepository;
    @InjectMocks
    private TipoArtigoService tipoArtigoService;

    private TipoArtigo criarTipo(Integer id, String nome) {
        TipoArtigo tipoArtigo = new TipoArtigo();
        ReflectionTestUtils.setField(tipoArtigo, "id", id);
        tipoArtigo.setNome(nome);
        return tipoArtigo;
    }

    @Test
    void criarGuardaNomeSemEspacosNasPontas() {
        when(tipoArtigoRepository.findByNomeIgnoreCase("Bebidas")).thenReturn(Optional.empty());
        when(tipoArtigoRepository.saveAndFlush(any(TipoArtigo.class))).thenAnswer(pedido -> {
            TipoArtigo guardado = pedido.getArgument(0);
            ReflectionTestUtils.setField(guardado, "id", 5);
            return guardado;
        });

        TipoArtigoResposta resposta = tipoArtigoService.criar(new TipoArtigoCriarPedido("  Bebidas "));

        assertThat(resposta).isEqualTo(new TipoArtigoResposta(5, "Bebidas"));
    }

    @Test
    void criarComNomeDuplicadoComOutrasMaiusculasDaConflito() {
        when(tipoArtigoRepository.findByNomeIgnoreCase("BEBIDAS")).thenReturn(Optional.of(criarTipo(1, "Bebidas")));

        assertThatThrownBy(() -> tipoArtigoService.criar(new TipoArtigoCriarPedido("BEBIDAS")))
                .isInstanceOf(ConflitoException.class)
                .hasMessage(NOME_EM_USO);
        verify(tipoArtigoRepository, never()).saveAndFlush(any());
    }

    @Test
    void criarQueViolaOIndiceUnicoDaConflito() {
        when(tipoArtigoRepository.findByNomeIgnoreCase("Bebidas")).thenReturn(Optional.empty());
        when(tipoArtigoRepository.saveAndFlush(any(TipoArtigo.class)))
                .thenThrow(new DataIntegrityViolationException("uq_tipo_artigo_nome"));

        assertThatThrownBy(() -> tipoArtigoService.criar(new TipoArtigoCriarPedido("Bebidas")))
                .isInstanceOf(ConflitoException.class)
                .hasMessage(NOME_EM_USO);
    }

    @Test
    void atualizarAlteraONome() {
        TipoArtigo tipoArtigo = criarTipo(1, "Bebidas");
        when(tipoArtigoRepository.findById(1)).thenReturn(Optional.of(tipoArtigo));
        when(tipoArtigoRepository.findByNomeIgnoreCase("Sumos")).thenReturn(Optional.empty());
        when(tipoArtigoRepository.saveAndFlush(tipoArtigo)).thenReturn(tipoArtigo);

        TipoArtigoResposta resposta = tipoArtigoService.atualizar(1, new TipoArtigoAtualizarPedido("Sumos"));

        assertThat(resposta).isEqualTo(new TipoArtigoResposta(1, "Sumos"));
    }

    @Test
    void atualizarIgnoraOProprioNaVerificacaoDoNome() {
        TipoArtigo tipoArtigo = criarTipo(1, "Bebidas");
        when(tipoArtigoRepository.findById(1)).thenReturn(Optional.of(tipoArtigo));
        when(tipoArtigoRepository.findByNomeIgnoreCase("BEBIDAS")).thenReturn(Optional.of(tipoArtigo));
        when(tipoArtigoRepository.saveAndFlush(tipoArtigo)).thenReturn(tipoArtigo);

        TipoArtigoResposta resposta = tipoArtigoService.atualizar(1, new TipoArtigoAtualizarPedido("BEBIDAS"));

        assertThat(resposta.nome()).isEqualTo("BEBIDAS");
    }

    @Test
    void atualizarComNomeDeOutroDaConflito() {
        when(tipoArtigoRepository.findById(1)).thenReturn(Optional.of(criarTipo(1, "Bebidas")));
        when(tipoArtigoRepository.findByNomeIgnoreCase("LIMPEZA")).thenReturn(Optional.of(criarTipo(2, "Limpeza")));

        assertThatThrownBy(() -> tipoArtigoService.atualizar(1, new TipoArtigoAtualizarPedido("LIMPEZA")))
                .isInstanceOf(ConflitoException.class)
                .hasMessage(NOME_EM_USO);
        verify(tipoArtigoRepository, never()).saveAndFlush(any());
    }

    @Test
    void atualizarInexistenteDaNaoEncontrado() {
        when(tipoArtigoRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoArtigoService.atualizar(9, new TipoArtigoAtualizarPedido("Bebidas")))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("O tipo de artigo não existe.");
    }

    @Test
    void apagarFazFlushDentroDoServico() {
        TipoArtigo tipoArtigo = criarTipo(1, "Bebidas");
        when(tipoArtigoRepository.findById(1)).thenReturn(Optional.of(tipoArtigo));

        tipoArtigoService.apagar(1);

        verify(tipoArtigoRepository).delete(tipoArtigo);
        verify(tipoArtigoRepository).flush();
    }

    @Test
    void apagarInexistenteDaNaoEncontrado() {
        when(tipoArtigoRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tipoArtigoService.apagar(9))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("O tipo de artigo não existe.");
    }

    @Test
    void apagarComArtigosAssociadosDaConflito() {
        when(tipoArtigoRepository.findById(1)).thenReturn(Optional.of(criarTipo(1, "Bebidas")));
        doThrow(new DataIntegrityViolationException("fk")).when(tipoArtigoRepository).flush();

        assertThatThrownBy(() -> tipoArtigoService.apagar(1))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Não é possível apagar um tipo de artigo com artigos associados.");
    }
}
