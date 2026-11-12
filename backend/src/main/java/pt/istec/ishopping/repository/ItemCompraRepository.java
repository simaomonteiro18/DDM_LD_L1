package pt.istec.ishopping.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import pt.istec.ishopping.model.ItemCompra;

public interface ItemCompraRepository extends JpaRepository<ItemCompra, Integer> {

    @EntityGraph(attributePaths = { "artigo", "artigo.tipoArtigo" })
    List<ItemCompra> findByCompraIdOrderByIdAsc(Integer idCompra);

    @EntityGraph(attributePaths = { "artigo", "artigo.tipoArtigo" })
    Optional<ItemCompra> findByIdAndCompraId(Integer id, Integer idCompra);
}
