package pt.istec.ishopping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UtilizadorCriarPedido(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 100, message = "O nome não pode ter mais de 100 caracteres.")
        String nome,

        @NotBlank(message = "O username é obrigatório.")
        @Size(max = 50, message = "O username não pode ter mais de 50 caracteres.")
        @Pattern(regexp = "(?U)\\S*", message = "O username não pode conter espaços.")
        String username,

        @NotNull(message = "A palavra-passe é obrigatória.")
        @PalavraPasseValida
        String password) {

    public UtilizadorCriarPedido {
        nome = nome == null ? null : nome.strip();
        username = username == null ? null : username.strip();
        password = password == null || password.isEmpty() ? null : password;
    }

    @Override
    public String toString() {
        return "UtilizadorCriarPedido[nome=" + nome + ", username=" + username + "]";
    }
}
