package pt.istec.ishopping.repository;

import java.math.BigDecimal;
import java.time.Instant;

public record GastoCompra(Integer idCompra, Instant dataCriacao, BigDecimal total) {
}
