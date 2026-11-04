package pt.istec.ishopping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ArtigoAtualizarPedido(
        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 100, message = "O nome não pode ter mais de 100 caracteres.")
        String nome,

        @NotNull(message = "O tipo de artigo é obrigatório.")
        Integer idTipoArtigo) {

    public ArtigoAtualizarPedido {
        nome = nome == null ? null : nome.strip();
    }
}
