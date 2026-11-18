package pt.istec.ishopping.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pt.istec.ishopping.model.Orcamento;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Integer> {

    @Override
    @EntityGraph(attributePaths = { "criadoPor", "alteradoPor" })
    Optional<Orcamento> findById(Integer id);

    @Query("select o from Orcamento o join fetch o.criadoPor left join fetch o.alteradoPor"
            + " order by o.ano desc, o.mes desc")
    List<Orcamento> findAllComUtilizadores();

    @Query("select o from Orcamento o join fetch o.criadoPor left join fetch o.alteradoPor"
            + " where o.ano = :ano order by o.mes desc")
    List<Orcamento> findAllComUtilizadoresPorAno(@Param("ano") Integer ano);

    Optional<Orcamento> findByAnoAndMes(Integer ano, Integer mes);

    List<Orcamento> findByAno(Integer ano);
}
