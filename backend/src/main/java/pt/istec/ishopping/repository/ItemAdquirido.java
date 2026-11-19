package pt.istec.ishopping.repository;

import java.time.Instant;

public record ItemAdquirido(Integer idCompra, Instant dataCriacaoCompra, Integer idArtigo, String nomeArtigo,
        Integer idTipoArtigo, String nomeTipoArtigo, Integer quantidadeAdquirida) {
}
