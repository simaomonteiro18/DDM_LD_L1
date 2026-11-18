package pt.istec.ishopping.repository;

import java.math.BigDecimal;

public record ResumoItensCompra(Integer idCompra, Long totalItens, Long itensPrevistos, Long itensNaoPrevistos,
        BigDecimal totalGasto) {
}
