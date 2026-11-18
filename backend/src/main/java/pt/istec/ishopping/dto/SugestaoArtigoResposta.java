package pt.istec.ishopping.dto;

public record SugestaoArtigoResposta(
        Integer idArtigo,
        String nomeArtigo,
        TipoArtigoResposta tipoArtigo,
        long vezes,
        int quantidadeSugerida) {
}
