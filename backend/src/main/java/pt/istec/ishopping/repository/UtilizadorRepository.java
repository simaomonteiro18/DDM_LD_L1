package pt.istec.ishopping.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pt.istec.ishopping.model.Utilizador;

public interface UtilizadorRepository extends JpaRepository<Utilizador, Integer> {

    @Query("select u from Utilizador u where lower(u.username) = lower(:username)")
    Optional<Utilizador> findByUsernameIgnoreCase(@Param("username") String username);
}
