package pt.istec.ishopping.dto;

import java.math.BigDecimal;

import pt.istec.ishopping.model.ItemCompra;
import pt.istec.ishopping.model.ItemCompraNaoPrevisto;
import pt.istec.ishopping.model.ItemCompraPrevisto;

public record ItemCompraResposta(
        Integer id,
        TipoItemCompra tipo,
        ArtigoResposta artigo,
        Integer quantidadePrevista,
        Integer quantidadeAdquirida,
        BigDecimal precoUnitario,
        String observacoes) {

    public static ItemCompraResposta de(ItemCompra item) {
        ArtigoResposta artigo = ArtigoResposta.de(item.getArtigo());
        if (item instanceof ItemCompraPrevisto previsto) {
            return new ItemCompraResposta(item.getId(), TipoItemCompra.PREVISTO, artigo,
                    previsto.getQuantidadePrevista(), item.getQuantidadeAdquirida(), item.getPrecoUnitario(), null);
        }
        ItemCompraNaoPrevisto naoPrevisto = (ItemCompraNaoPrevisto) item;
        return new ItemCompraResposta(item.getId(), TipoItemCompra.NAO_PREVISTO, artigo,
                null, item.getQuantidadeAdquirida(), item.getPrecoUnitario(), naoPrevisto.getObservacoes());
    }
}
