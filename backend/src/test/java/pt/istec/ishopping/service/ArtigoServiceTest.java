package pt.istec.ishopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import pt.istec.ishopping.dto.ArtigoAtualizarPedido;
import pt.istec.ishopping.dto.ArtigoCriarPedido;
import pt.istec.ishopping.dto.ArtigoResposta;
import pt.istec.ishopping.dto.TipoArtigoResposta;
import pt.istec.ishopping.exception.ConflitoException;
import pt.istec.ishopping.exception.RecursoNaoEncontradoException;
import pt.istec.ishopping.model.Artigo;
import pt.istec.ishopping.model.TipoArtigo;
import pt.istec.ishopping.repository.ArtigoRepository;
import pt.istec.ishopping.repository.TipoArtigoRepository;

@ExtendWith(MockitoExtension.class)
class ArtigoServiceTest {

    private static final String NOME_EM_USO = "Já existe um artigo com este nome neste tipo de artigo.";

    @Mock
    private ArtigoRepository artigoRepository;
    @Mock
    private TipoArtigoRepository tipoArtigoRepository;
    @InjectMocks
    private ArtigoService artigoService;

    private TipoArtigo criarTipo(Integer id, String nome) {
        TipoArtigo tipoArtigo = new TipoArtigo();
        ReflectionTestUtils.setField(tipoArtigo, "id", id);
        tipoArtigo.setNome(nome);
        return tipoArtigo;
    }

    private Artigo criarArtigo(Integer id, String nome, TipoArtigo tipoArtigo) {
        Artigo artigo = new Artigo();
        ReflectionTestUtils.setField(artigo, "id", id);
        artigo.setNome(nome);
        artigo.setTipoArtigo(tipoArtigo);
        return artigo;
    }

    @Test
    void listarSemFiltroDevolveTodosComOTipo() {
        Artigo artigo = criarArtigo(1, "Arroz", criarTipo(2, "Mercearia"));
        when(artigoRepository.findAllComTipo()).thenReturn(List.of(artigo));

        assertThat(artigoService.listar(null))
                .containsExactly(new ArtigoResposta(1, "Arroz", new TipoArtigoResposta(2, "Mercearia")));
    }

    @Test
    void listarComFiltroUsaOTipoIndicado() {
        when(artigoRepository.findAllComTipoPorIdTipoArtigo(2)).thenReturn(List.of());

        assertThat(artigoService.listar(2)).isEmpty();
        verify(artigoRepository, never()).findAllComTipo();
    }

    @Test
    void criarGuardaNomeSemEspacosEOTipo() {
        when(tipoArtigoRepository.findById(2)).thenReturn(Optional.of(criarTipo(2, "Mercearia")));
        when(artigoRepository.findByIdTipoArtigoAndNomeIgnoreCase(2, "Arroz")).thenReturn(Optional.empty());
        when(artigoRepository.saveAndFlush(any(Artigo.class))).thenAnswer(pedido -> {
            Artigo guardado = pedido.getArgument(0);
            ReflectionTestUtils.setField(guardado, "id", 5);
            return guardado;
        });

        ArtigoResposta resposta = artigoService.criar(new ArtigoCriarPedido("  Arroz ", 2));

        assertThat(resposta).isEqualTo(new ArtigoResposta(5, "Arroz", new TipoArtigoResposta(2, "Mercearia")));
    }

    @Test
    void criarComTipoInexistenteDaNaoEncontrado() {
        when(tipoArtigoRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artigoService.criar(new ArtigoCriarPedido("Arroz", 9)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("O tipo de artigo não existe.");
        verify(artigoRepository, never()).saveAndFlush(any());
    }

    @Test
    void criarComNomeDuplicadoNoMesmoTipoDaConflito() {
        TipoArtigo tipoArtigo = criarTipo(2, "Mercearia");
        when(tipoArtigoRepository.findById(2)).thenReturn(Optional.of(tipoArtigo));
        when(artigoRepository.findByIdTipoArtigoAndNomeIgnoreCase(2, "ARROZ"))
                .thenReturn(Optional.of(criarArtigo(1, "Arroz", tipoArtigo)));

        assertThatThrownBy(() -> artigoService.criar(new ArtigoCriarPedido("ARROZ", 2)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage(NOME_EM_USO);
        verify(artigoRepository, never()).saveAndFlush(any());
    }

    @Test
    void criarQueViolaOIndiceUnicoDaConflito() {
        when(tipoArtigoRepository.findById(2)).thenReturn(Optional.of(criarTipo(2, "Mercearia")));
        when(artigoRepository.findByIdTipoArtigoAndNomeIgnoreCase(2, "Arroz")).thenReturn(Optional.empty());
        when(artigoRepository.saveAndFlush(any(Artigo.class)))
                .thenThrow(new DataIntegrityViolationException("uq_artigo_tipo_nome"));

        assertThatThrownBy(() -> artigoService.criar(new ArtigoCriarPedido("Arroz", 2)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage(NOME_EM_USO);
    }

    @Test
    void atualizarAlteraNomeETipo() {
        Artigo artigo = criarArtigo(1, "Arroz", criarTipo(2, "Mercearia"));
        when(artigoRepository.findById(1)).thenReturn(Optional.of(artigo));
        when(tipoArtigoRepository.findById(3)).thenReturn(Optional.of(criarTipo(3, "Limpeza")));
        when(artigoRepository.findByIdTipoArtigoAndNomeIgnoreCase(3, "Sabão")).thenReturn(Optional.empty());
        when(artigoRepository.saveAndFlush(artigo)).thenReturn(artigo);

        ArtigoResposta resposta = artigoService.atualizar(1, new ArtigoAtualizarPedido("Sabão", 3));

        assertThat(resposta).isEqualTo(new ArtigoResposta(1, "Sabão", new TipoArtigoResposta(3, "Limpeza")));
    }

    @Test
    void atualizarIgnoraOProprioNaVerificacaoDoNome() {
        TipoArtigo tipoArtigo = criarTipo(2, "Mercearia");
        Artigo artigo = criarArtigo(1, "Arroz", tipoArtigo);
        when(artigoRepository.findById(1)).thenReturn(Optional.of(artigo));
        when(tipoArtigoRepository.findById(2)).thenReturn(Optional.of(tipoArtigo));
        when(artigoRepository.findByIdTipoArtigoAndNomeIgnoreCase(2, "ARROZ")).thenReturn(Optional.of(artigo));
        when(artigoRepository.saveAndFlush(artigo)).thenReturn(artigo);

        assertThat(artigoService.atualizar(1, new ArtigoAtualizarPedido("ARROZ", 2)).nome()).isEqualTo("ARROZ");
    }

    @Test
    void atualizarComNomeDeOutroNoTipoDeDestinoDaConflito() {
        TipoArtigo destino = criarTipo(3, "Limpeza");
        when(artigoRepository.findById(1)).thenReturn(Optional.of(criarArtigo(1, "Sabão", criarTipo(2, "Higiene"))));
        when(tipoArtigoRepository.findById(3)).thenReturn(Optional.of(destino));
        when(artigoRepository.findByIdTipoArtigoAndNomeIgnoreCase(3, "Sabão"))
                .thenReturn(Optional.of(criarArtigo(7, "Sabão", destino)));

        assertThatThrownBy(() -> artigoService.atualizar(1, new ArtigoAtualizarPedido("Sabão", 3)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage(NOME_EM_USO);
        verify(artigoRepository, never()).saveAndFlush(any());
    }

    @Test
    void atualizarInexistenteDaNaoEncontrado() {
        when(artigoRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artigoService.atualizar(9, new ArtigoAtualizarPedido("Arroz", 2)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("O artigo não existe.");
    }

    @Test
    void atualizarComTipoInexistenteDaNaoEncontrado() {
        when(artigoRepository.findById(1)).thenReturn(Optional.of(criarArtigo(1, "Arroz", criarTipo(2, "Mercearia"))));
        when(tipoArtigoRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artigoService.atualizar(1, new ArtigoAtualizarPedido("Arroz", 9)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("O tipo de artigo não existe.");
    }

    @Test
    void apagarFazFlushDentroDoServico() {
        Artigo artigo = criarArtigo(1, "Arroz", criarTipo(2, "Mercearia"));
        when(artigoRepository.findById(1)).thenReturn(Optional.of(artigo));

        artigoService.apagar(1);

        verify(artigoRepository).delete(artigo);
        verify(artigoRepository).flush();
    }

    @Test
    void apagarInexistenteDaNaoEncontrado() {
        when(artigoRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> artigoService.apagar(9))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("O artigo não existe.");
    }

    @Test
    void apagarComItensAssociadosDaConflito() {
        when(artigoRepository.findById(1)).thenReturn(Optional.of(criarArtigo(1, "Arroz", criarTipo(2, "Mercearia"))));
        doThrow(new DataIntegrityViolationException("fk")).when(artigoRepository).flush();

        assertThatThrownBy(() -> artigoService.apagar(1))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Não é possível apagar um artigo com itens de compra associados.");
    }
}
