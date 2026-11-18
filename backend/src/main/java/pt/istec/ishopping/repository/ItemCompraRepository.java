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

    @Query("select new pt.istec.ishopping.repository.GastoCompra(c.id, c.dataCriacao,"
            + " sum(i.quantidadeAdquirida * i.precoUnitario)) from ItemCompra i join i.compra c"
            + " where c.dataCriacao >= :inicio and c.dataCriacao < :fim group by c.id, c.dataCriacao")
    List<GastoCompra> somarGastoPorCompraEntre(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

    @Query("select i from ItemCompra i join fetch i.compra c join fetch i.artigo"
            + " where c.fechada = true and c.criadoPor.id = :idUtilizador"
            + " order by c.dataFechada desc, c.id desc, i.id asc")
    List<ItemCompra> findItensDeComprasFechadasDoUtilizador(@Param("idUtilizador") Integer idUtilizador);

    @Query("select new pt.istec.ishopping.repository.ItemAdquirido(c.id, c.dataCriacao, a.id, a.nome, t.id, t.nome,"
            + " i.quantidadeAdquirida) from ItemCompra i join i.compra c join i.artigo a join a.tipoArtigo t"
            + " where c.fechada = true and i.quantidadeAdquirida is not null")
    List<ItemAdquirido> findItensAdquiridosDeComprasFechadas();

    @Query("select new pt.istec.ishopping.repository.ResumoItensCompra(i.compra.id, count(i),"
            + " sum(case when type(i) = ItemCompraPrevisto then 1 else 0 end),"
            + " sum(case when type(i) = ItemCompraNaoPrevisto then 1 else 0 end),"
            + " sum(i.quantidadeAdquirida * i.precoUnitario))"
            + " from ItemCompra i where i.compra.id in :idsCompras group by i.compra.id")
    List<ResumoItensCompra> resumirPorCompra(@Param("idsCompras") Collection<Integer> idsCompras);

    @Query("select sum(i.quantidadeAdquirida * i.precoUnitario) from ItemCompra i"
            + " where i.compra.dataCriacao >= :inicio and i.compra.dataCriacao < :fim")
    BigDecimal somarGastoEntre(@Param("inicio") Instant inicio, @Param("fim") Instant fim);
}
