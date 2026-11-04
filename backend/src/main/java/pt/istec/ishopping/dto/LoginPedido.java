package pt.istec.ishopping.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginPedido(
        @NotBlank(message = "O username é obrigatório.") String username,
        @NotBlank(message = "A palavra-passe é obrigatória.") String password) {

    @Override
    public String toString() {
        return "LoginPedido[username=" + username + "]";
    }
}
