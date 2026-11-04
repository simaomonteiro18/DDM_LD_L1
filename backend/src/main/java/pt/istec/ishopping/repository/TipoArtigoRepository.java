package pt.istec.ishopping.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pt.istec.ishopping.model.TipoArtigo;

public interface TipoArtigoRepository extends JpaRepository<TipoArtigo, Integer> {

    @Query("select t from TipoArtigo t where lower(t.nome) = lower(:nome)")
    Optional<TipoArtigo> findByNomeIgnoreCase(@Param("nome") String nome);
}
