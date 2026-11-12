package pt.istec.ishopping.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.istec.ishopping.dto.ItemCompraAtualizarPedido;
import pt.istec.ishopping.dto.ItemCompraResposta;
import pt.istec.ishopping.dto.ItemNaoPrevistoCriarPedido;
import pt.istec.ishopping.dto.ItemPrevistoCriarPedido;
import pt.istec.ishopping.exception.ConflitoException;
import pt.istec.ishopping.exception.PedidoInvalidoException;
import pt.istec.ishopping.exception.RecursoNaoEncontradoException;
import pt.istec.ishopping.model.Artigo;
import pt.istec.ishopping.model.Compra;
import pt.istec.ishopping.model.ItemCompra;
import pt.istec.ishopping.model.ItemCompraNaoPrevisto;
import pt.istec.ishopping.model.ItemCompraPrevisto;
import pt.istec.ishopping.repository.ArtigoRepository;
import pt.istec.ishopping.repository.CompraRepository;
import pt.istec.ishopping.repository.ItemCompraRepository;

@Service
public class ItemCompraService {

    private static final String COMPRA_NAO_EXISTE = "A compra não existe.";
    private static final String ITEM_NAO_EXISTE = "O item de compra não existe.";

    private final ItemCompraRepository itemCompraRepository;
    private final CompraRepository compraRepository;
    private final ArtigoRepository artigoRepository;

    public ItemCompraService(ItemCompraRepository itemCompraRepository, CompraRepository compraRepository,
            ArtigoRepository artigoRepository) {
        this.itemCompraRepository = itemCompraRepository;
        this.compraRepository = compraRepository;
        this.artigoRepository = artigoRepository;
    }

    @Transactional(readOnly = true)
    public List<ItemCompraResposta> listar(Integer idCompra) {
        verificarCompraExiste(idCompra);
        return itemCompraRepository.findByCompraIdOrderByIdAsc(idCompra).stream()
                .map(ItemCompraResposta::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public ItemCompraResposta obter(Integer idCompra, Integer idItem) {
        verificarCompraExiste(idCompra);
        return ItemCompraResposta.de(procurarItem(idCompra, idItem));
    }

    @Transactional
    public ItemCompraResposta criarPrevisto(Integer idCompra, ItemPrevistoCriarPedido pedido) {
        Compra compra = procurarCompraAberta(idCompra, "Não é possível adicionar itens a uma compra fechada.");
        ItemCompraPrevisto item = new ItemCompraPrevisto();
        item.setCompra(compra);
        item.setArtigo(procurarArtigo(pedido.idArtigo()));
        item.setQuantidadePrevista(pedido.quantidadePrevista());
        item.setQuantidadeAdquirida(pedido.quantidadeAdquirida());
        item.setPrecoUnitario(normalizar(pedido.precoUnitario()));
        return ItemCompraResposta.de(itemCompraRepository.saveAndFlush(item));
    }

    @Transactional
    public ItemCompraResposta criarNaoPrevisto(Integer idCompra, ItemNaoPrevistoCriarPedido pedido) {
        Compra compra = procurarCompraAberta(idCompra, "Não é possível adicionar itens a uma compra fechada.");
        ItemCompraNaoPrevisto item = new ItemCompraNaoPrevisto();
        item.setCompra(compra);
        item.setArtigo(procurarArtigo(pedido.idArtigo()));
        item.setQuantidadeAdquirida(pedido.quantidadeAdquirida());
        item.setPrecoUnitario(normalizar(pedido.precoUnitario()));
        item.setObservacoes(pedido.observacoes());
        return ItemCompraResposta.de(itemCompraRepository.saveAndFlush(item));
    }

    @Transactional
    public ItemCompraResposta atualizar(Integer idCompra, Integer idItem, ItemCompraAtualizarPedido pedido) {
        procurarCompraAberta(idCompra, "Não é possível alterar itens de uma compra fechada.");
        ItemCompra item = procurarItem(idCompra, idItem);
        if (item instanceof ItemCompraPrevisto previsto) {
            atualizarPrevisto(previsto, pedido);
        } else {
            atualizarNaoPrevisto((ItemCompraNaoPrevisto) item, pedido);
        }
        item.setArtigo(procurarArtigo(pedido.idArtigo()));
        item.setQuantidadeAdquirida(pedido.quantidadeAdquirida());
        item.setPrecoUnitario(normalizar(pedido.precoUnitario()));
        return ItemCompraResposta.de(itemCompraRepository.saveAndFlush(item));
    }

    @Transactional
    public void apagar(Integer idCompra, Integer idItem) {
        procurarCompraAberta(idCompra, "Não é possível apagar itens de uma compra fechada.");
        ItemCompra item = procurarItem(idCompra, idItem);
        itemCompraRepository.delete(item);
        itemCompraRepository.flush();
    }

    private void atualizarPrevisto(ItemCompraPrevisto item, ItemCompraAtualizarPedido pedido) {
        if (pedido.quantidadePrevista() == null) {
            throw new PedidoInvalidoException("A quantidade prevista é obrigatória.");
        }
        if (pedido.observacoes() != null) {
            throw new PedidoInvalidoException("Um item previsto não tem observações.");
        }
        item.setQuantidadePrevista(pedido.quantidadePrevista());
    }

    private void atualizarNaoPrevisto(ItemCompraNaoPrevisto item, ItemCompraAtualizarPedido pedido) {
        if (pedido.quantidadePrevista() != null) {
            throw new PedidoInvalidoException("Um item não previsto não tem quantidade prevista.");
        }
        if (pedido.quantidadeAdquirida() == null) {
            throw new PedidoInvalidoException(
                    "Um item não previsto tem de ter a quantidade adquirida e o preço unitário.");
        }
        item.setObservacoes(pedido.observacoes());
    }

    private void verificarCompraExiste(Integer idCompra) {
        if (!compraRepository.existsById(idCompra)) {
            throw new RecursoNaoEncontradoException(COMPRA_NAO_EXISTE);
        }
    }

    private Compra procurarCompraAberta(Integer idCompra, String mensagemSeFechada) {
        Compra compra = compraRepository.findByIdParaAlterar(idCompra)
                .orElseThrow(() -> new RecursoNaoEncontradoException(COMPRA_NAO_EXISTE));
        if (compra.isFechada()) {
            throw new ConflitoException(mensagemSeFechada);
        }
        return compra;
    }

    private ItemCompra procurarItem(Integer idCompra, Integer idItem) {
        return itemCompraRepository.findByIdAndCompraId(idItem, idCompra)
                .orElseThrow(() -> new RecursoNaoEncontradoException(ITEM_NAO_EXISTE));
    }

    private Artigo procurarArtigo(Integer idArtigo) {
        return artigoRepository.findById(idArtigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("O artigo não existe."));
    }

    private BigDecimal normalizar(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.UNNECESSARY);
    }
}
