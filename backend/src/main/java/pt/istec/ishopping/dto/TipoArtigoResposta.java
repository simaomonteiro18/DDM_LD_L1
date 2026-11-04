package pt.istec.ishopping.dto;

import pt.istec.ishopping.model.TipoArtigo;

public record TipoArtigoResposta(Integer id, String nome) {

    public static TipoArtigoResposta de(TipoArtigo tipoArtigo) {
        return new TipoArtigoResposta(tipoArtigo.getId(), tipoArtigo.getNome());
    }
}
