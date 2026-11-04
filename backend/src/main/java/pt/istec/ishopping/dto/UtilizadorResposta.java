package pt.istec.ishopping.dto;

import pt.istec.ishopping.model.Utilizador;

public record UtilizadorResposta(Integer id, String nome, String username) {

    public static UtilizadorResposta de(Utilizador utilizador) {
        return new UtilizadorResposta(utilizador.getId(), utilizador.getNome(), utilizador.getUsername());
    }
}
