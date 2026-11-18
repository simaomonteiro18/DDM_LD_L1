package pt.istec.ishopping.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pt.istec.ishopping.model.ItemCompra;

public interface ItemCompraRepository extends JpaRepository<ItemCompra, Integer> {

    @EntityGraph(attributePaths = { "artigo", "artigo.tipoArtigo" })
    List<ItemCompra> findByCompraIdOrderByIdAsc(Integer idCompra);

    @EntityGraph(attributePaths = { "artigo", "artigo.tipoArtigo" })
    Optional<ItemCompra> findByIdAndCompraId(Integer id, Integer idCompra);

    @Query("select count(p) from ItemCompraPrevisto p where p.compra.id = :idCompra")
    long contarPrevistos(@Param("idCompra") Integer idCompra);

    @Query("select new pt.istec.ishopping.repository.TotalItensCompra(p.compra.id, count(p))"
            + " from ItemCompraPrevisto p where p.compra.id in :idsCompras group by p.compra.id")
    List<TotalItensCompra> contarPrevistosPorCompra(@Param("idsCompras") Collection<Integer> idsCompras);

    @Query("select sum(i.quantidadeAdquirida * i.precoUnitario) from ItemCompra i"
            + " where i.compra.dataCriacao >= :inicio and i.compra.dataCriacao < :fim")
    BigDecimal somarGastoEntre(@Param("inicio") Instant inicio, @Param("fim") Instant fim);
}
