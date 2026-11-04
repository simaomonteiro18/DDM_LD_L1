package pt.istec.ishopping.dto;

import pt.istec.ishopping.model.Artigo;

public record ArtigoResposta(Integer id, String nome, TipoArtigoResposta tipo) {

    public static ArtigoResposta de(Artigo artigo) {
        return new ArtigoResposta(artigo.getId(), artigo.getNome(), TipoArtigoResposta.de(artigo.getTipoArtigo()));
    }
}
