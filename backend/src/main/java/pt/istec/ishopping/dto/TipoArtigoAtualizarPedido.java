package pt.istec.ishopping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoArtigoAtualizarPedido(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 100, message = "O nome não pode ter mais de 100 caracteres.")
        String nome) {

    public TipoArtigoAtualizarPedido {
        nome = nome == null ? null : nome.strip();
    }
}
