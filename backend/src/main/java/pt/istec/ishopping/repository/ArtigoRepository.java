package pt.istec.ishopping.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pt.istec.ishopping.model.Artigo;

public interface ArtigoRepository extends JpaRepository<Artigo, Integer> {

    @Override
    @EntityGraph(attributePaths = "tipoArtigo")
    Optional<Artigo> findById(Integer id);

    @Query("select a from Artigo a join fetch a.tipoArtigo order by a.nome, a.id")
    List<Artigo> findAllComTipo();

    @Query("select a from Artigo a join fetch a.tipoArtigo t where t.id = :idTipoArtigo order by a.nome, a.id")
    List<Artigo> findAllComTipoPorIdTipoArtigo(@Param("idTipoArtigo") Integer idTipoArtigo);

    @Query("select a from Artigo a where a.tipoArtigo.id = :idTipoArtigo and lower(a.nome) = lower(:nome)")
    Optional<Artigo> findByIdTipoArtigoAndNomeIgnoreCase(@Param("idTipoArtigo") Integer idTipoArtigo,
            @Param("nome") String nome);
}
