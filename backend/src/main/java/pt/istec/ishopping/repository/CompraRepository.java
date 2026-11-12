package pt.istec.ishopping.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import pt.istec.ishopping.model.Compra;

public interface CompraRepository extends JpaRepository<Compra, Integer> {

    @Override
    @EntityGraph(attributePaths = { "criadoPor", "alteradoPor", "fechadaPor" })
    Optional<Compra> findById(Integer id);

    @EntityGraph(attributePaths = { "criadoPor", "alteradoPor", "fechadaPor" })
    List<Compra> findAllByOrderByDataCriacaoDescIdDesc();

    @EntityGraph(attributePaths = { "criadoPor", "alteradoPor", "fechadaPor" })
    List<Compra> findByFechadaOrderByDataCriacaoDescIdDesc(boolean fechada);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Compra c where c.id = :id")
    Optional<Compra> findByIdParaAlterar(@Param("id") Integer id);
}
