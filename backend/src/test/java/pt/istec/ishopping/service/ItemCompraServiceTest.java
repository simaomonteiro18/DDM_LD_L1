package pt.istec.ishopping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import pt.istec.ishopping.dto.ItemCompraAtualizarPedido;
import pt.istec.ishopping.dto.ItemCompraResposta;
import pt.istec.ishopping.dto.ItemNaoPrevistoCriarPedido;
import pt.istec.ishopping.dto.ItemPrevistoCriarPedido;
import pt.istec.ishopping.dto.TipoItemCompra;
import pt.istec.ishopping.exception.ConflitoException;
import pt.istec.ishopping.exception.PedidoInvalidoException;
import pt.istec.ishopping.exception.RecursoNaoEncontradoException;
import pt.istec.ishopping.model.Artigo;
import pt.istec.ishopping.model.Compra;
import pt.istec.ishopping.model.ItemCompra;
import pt.istec.ishopping.model.ItemCompraNaoPrevisto;
import pt.istec.ishopping.model.ItemCompraPrevisto;
import pt.istec.ishopping.model.TipoArtigo;
import pt.istec.ishopping.model.Utilizador;
import pt.istec.ishopping.repository.ArtigoRepository;
import pt.istec.ishopping.repository.CompraRepository;
import pt.istec.ishopping.repository.ItemCompraRepository;

@ExtendWith(MockitoExtension.class)
class ItemCompraServiceTest {

    @Mock
    private ItemCompraRepository itemCompraRepository;
    @Mock
    private CompraRepository compraRepository;
    @Mock
    private ArtigoRepository artigoRepository;
    @InjectMocks
    private ItemCompraService itemCompraService;

    private Compra compra;
    private Artigo arroz;

    @BeforeEach
    void preparar() {
        compra = new Compra();
        ReflectionTestUtils.setField(compra, "id", 1);
        compra.setNome("Semana 1");
        TipoArtigo mercearia = new TipoArtigo();
        ReflectionTestUtils.setField(mercearia, "id", 2);
        mercearia.setNome("Mercearia");
        arroz = new Artigo();
        ReflectionTestUtils.setField(arroz, "id", 3);
        arroz.setNome("Arroz");
        arroz.setTipoArtigo(mercearia);
    }

    private void fecharCompra() {
        compra.fechar(new Utilizador());
    }

    private ItemCompraPrevisto criarPrevisto(Integer id) {
        ItemCompraPrevisto item = new ItemCompraPrevisto();
        ReflectionTestUtils.setField(item, "id", id);
        item.setCompra(compra);
        item.setArtigo(arroz);
        item.setQuantidadePrevista(2);
        return item;
    }

    private ItemCompraNaoPrevisto criarNaoPrevisto(Integer id) {
        ItemCompraNaoPrevisto item = new ItemCompraNaoPrevisto();
        ReflectionTestUtils.setField(item, "id", id);
        item.setCompra(compra);
        item.setArtigo(arroz);
        item.setQuantidadeAdquirida(1);
        item.setPrecoUnitario(new BigDecimal("1.99"));
        return item;
    }

    private void guardarComId(Integer id) {
        when(itemCompraRepository.saveAndFlush(any(ItemCompra.class))).thenAnswer(pedido -> {
            ItemCompra guardado = pedido.getArgument(0);
            ReflectionTestUtils.setField(guardado, "id", id);
            return guardado;
        });
    }

    @Test
    void listarDevolveOsDoisTiposDeItem() {
        when(compraRepository.existsById(1)).thenReturn(true);
        when(itemCompraRepository.findByCompraIdOrderByIdAsc(1)).thenReturn(List.of(criarPrevisto(1), criarNaoPrevisto(2)));

        assertThat(itemCompraService.listar(1))
                .extracting(ItemCompraResposta::tipo)
                .containsExactly(TipoItemCompra.PREVISTO, TipoItemCompra.NAO_PREVISTO);
    }

    @Test
    void listarDeCompraInexistenteDaNaoEncontrado() {
        when(compraRepository.existsById(9)).thenReturn(false);

        assertThatThrownBy(() -> itemCompraService.listar(9))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("A compra não existe.");
    }

    @Test
    void obterItemDeOutraCompraDaNaoEncontrado() {
        when(compraRepository.existsById(1)).thenReturn(true);
        when(itemCompraRepository.findByIdAndCompraId(5, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemCompraService.obter(1, 5))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("O item de compra não existe.");
    }

    @Test
    void criarPrevistoSemAquisicaoGuardaNaCompra() {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(artigoRepository.findById(3)).thenReturn(Optional.of(arroz));
        guardarComId(7);

        ItemCompraResposta resposta = itemCompraService.criarPrevisto(1, new ItemPrevistoCriarPedido(3, 2, null, null));

        assertThat(resposta.id()).isEqualTo(7);
        assertThat(resposta.tipo()).isEqualTo(TipoItemCompra.PREVISTO);
        assertThat(resposta.artigo().nome()).isEqualTo("Arroz");
        assertThat(resposta.quantidadePrevista()).isEqualTo(2);
        assertThat(resposta.quantidadeAdquirida()).isNull();
        assertThat(resposta.precoUnitario()).isNull();
    }

    @Test
    void criarNaoPrevistoGuardaAquisicaoEObservacoes() {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(artigoRepository.findById(3)).thenReturn(Optional.of(arroz));
        guardarComId(8);

        ItemCompraResposta resposta = itemCompraService.criarNaoPrevisto(1,
                new ItemNaoPrevistoCriarPedido(3, 1, new BigDecimal("1.5"), "  em promoção "));

        assertThat(resposta.tipo()).isEqualTo(TipoItemCompra.NAO_PREVISTO);
        assertThat(resposta.quantidadePrevista()).isNull();
        assertThat(resposta.quantidadeAdquirida()).isEqualTo(1);
        assertThat(resposta.precoUnitario()).isEqualTo(new BigDecimal("1.50"));
        assertThat(resposta.observacoes()).isEqualTo("em promoção");
    }

    @Test
    void criarEmCompraFechadaDaConflito() {
        fecharCompra();
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));

        assertThatThrownBy(() -> itemCompraService.criarPrevisto(1, new ItemPrevistoCriarPedido(3, 2, null, null)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Não é possível adicionar itens a uma compra fechada.");
        verify(itemCompraRepository, never()).saveAndFlush(any());
    }

    @Test
    void criarEmCompraInexistenteDaNaoEncontrado() {
        when(compraRepository.findByIdParaAlterar(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemCompraService.criarNaoPrevisto(9,
                new ItemNaoPrevistoCriarPedido(3, 1, BigDecimal.ONE, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("A compra não existe.");
    }

    @Test
    void criarComArtigoInexistenteDaNaoEncontrado() {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(artigoRepository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemCompraService.criarPrevisto(1, new ItemPrevistoCriarPedido(9, 2, null, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("O artigo não existe.");
        verify(itemCompraRepository, never()).saveAndFlush(any());
    }

    @Test
    void atualizarPrevistoRegistaAAquisicao() {
        ItemCompraPrevisto item = criarPrevisto(5);
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(itemCompraRepository.findByIdAndCompraId(5, 1)).thenReturn(Optional.of(item));
        when(artigoRepository.findById(3)).thenReturn(Optional.of(arroz));
        when(itemCompraRepository.saveAndFlush(item)).thenReturn(item);

        ItemCompraResposta resposta = itemCompraService.atualizar(1, 5,
                new ItemCompraAtualizarPedido(3, 3, 3, new BigDecimal("0.99"), null));

        assertThat(resposta.tipo()).isEqualTo(TipoItemCompra.PREVISTO);
        assertThat(resposta.quantidadePrevista()).isEqualTo(3);
        assertThat(resposta.quantidadeAdquirida()).isEqualTo(3);
        assertThat(resposta.precoUnitario()).isEqualTo(new BigDecimal("0.99"));
    }

    @Test
    void atualizarPrevistoSemQuantidadePrevistaDaPedidoInvalido() {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(itemCompraRepository.findByIdAndCompraId(5, 1)).thenReturn(Optional.of(criarPrevisto(5)));

        assertThatThrownBy(() -> itemCompraService.atualizar(1, 5, new ItemCompraAtualizarPedido(3, null, null, null, null)))
                .isInstanceOf(PedidoInvalidoException.class)
                .hasMessage("A quantidade prevista é obrigatória.");
    }

    @Test
    void atualizarPrevistoComObservacoesNaoMudaOTipo() {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(itemCompraRepository.findByIdAndCompraId(5, 1)).thenReturn(Optional.of(criarPrevisto(5)));

        assertThatThrownBy(() -> itemCompraService.atualizar(1, 5, new ItemCompraAtualizarPedido(3, 2, null, null, "x")))
                .isInstanceOf(PedidoInvalidoException.class)
                .hasMessage("Um item previsto não tem observações.");
        verify(itemCompraRepository, never()).saveAndFlush(any());
    }

    @Test
    void atualizarNaoPrevistoAlteraAsObservacoes() {
        ItemCompraNaoPrevisto item = criarNaoPrevisto(6);
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(itemCompraRepository.findByIdAndCompraId(6, 1)).thenReturn(Optional.of(item));
        when(artigoRepository.findById(3)).thenReturn(Optional.of(arroz));
        when(itemCompraRepository.saveAndFlush(item)).thenReturn(item);

        ItemCompraResposta resposta = itemCompraService.atualizar(1, 6,
                new ItemCompraAtualizarPedido(3, null, 4, new BigDecimal("2"), "outra marca"));

        assertThat(resposta.tipo()).isEqualTo(TipoItemCompra.NAO_PREVISTO);
        assertThat(resposta.quantidadeAdquirida()).isEqualTo(4);
        assertThat(resposta.precoUnitario()).isEqualTo(new BigDecimal("2.00"));
        assertThat(resposta.observacoes()).isEqualTo("outra marca");
    }

    @Test
    void atualizarNaoPrevistoComQuantidadePrevistaNaoMudaOTipo() {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(itemCompraRepository.findByIdAndCompraId(6, 1)).thenReturn(Optional.of(criarNaoPrevisto(6)));

        assertThatThrownBy(() -> itemCompraService.atualizar(1, 6,
                new ItemCompraAtualizarPedido(3, 2, 1, BigDecimal.ONE, null)))
                .isInstanceOf(PedidoInvalidoException.class)
                .hasMessage("Um item não previsto não tem quantidade prevista.");
    }

    @Test
    void atualizarNaoPrevistoSemAquisicaoDaPedidoInvalido() {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(itemCompraRepository.findByIdAndCompraId(6, 1)).thenReturn(Optional.of(criarNaoPrevisto(6)));

        assertThatThrownBy(() -> itemCompraService.atualizar(1, 6, new ItemCompraAtualizarPedido(3, null, null, null, null)))
                .isInstanceOf(PedidoInvalidoException.class)
                .hasMessage("Um item não previsto tem de ter a quantidade adquirida e o preço unitário.");
    }

    @Test
    void atualizarEmCompraFechadaDaConflito() {
        fecharCompra();
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));

        assertThatThrownBy(() -> itemCompraService.atualizar(1, 5, new ItemCompraAtualizarPedido(3, 2, null, null, null)))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Não é possível alterar itens de uma compra fechada.");
    }

    @Test
    void atualizarItemDeOutraCompraDaNaoEncontrado() {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(itemCompraRepository.findByIdAndCompraId(5, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemCompraService.atualizar(1, 5, new ItemCompraAtualizarPedido(3, 2, null, null, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("O item de compra não existe.");
    }

    @Test
    void apagarItemDeCompraAberta() {
        ItemCompraPrevisto item = criarPrevisto(5);
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(itemCompraRepository.findByIdAndCompraId(5, 1)).thenReturn(Optional.of(item));

        itemCompraService.apagar(1, 5);

        verify(itemCompraRepository).delete(item);
        verify(itemCompraRepository).flush();
    }

    @Test
    void apagarEmCompraFechadaDaConflito() {
        fecharCompra();
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));

        assertThatThrownBy(() -> itemCompraService.apagar(1, 5))
                .isInstanceOf(ConflitoException.class)
                .hasMessage("Não é possível apagar itens de uma compra fechada.");
        verify(itemCompraRepository, never()).delete(any());
    }

    @Test
    void apagarItemDeOutraCompraDaNaoEncontrado() {
        when(compraRepository.findByIdParaAlterar(1)).thenReturn(Optional.of(compra));
        when(itemCompraRepository.findByIdAndCompraId(5, 1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemCompraService.apagar(1, 5))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("O item de compra não existe.");
        verify(itemCompraRepository, never()).delete(any());
    }
}
