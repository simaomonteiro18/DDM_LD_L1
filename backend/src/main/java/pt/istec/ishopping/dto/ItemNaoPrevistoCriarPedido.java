package pt.istec.ishopping.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemNaoPrevistoCriarPedido(
        @NotNull(message = "O artigo é obrigatório.")
        Integer idArtigo,

        @NotNull(message = "A quantidade adquirida é obrigatória.")
        @Positive(message = "A quantidade adquirida tem de ser maior que zero.")
        Integer quantidadeAdquirida,

        @NotNull(message = "O preço unitário é obrigatório.")
        @DecimalMin(value = "0", message = "O preço unitário não pode ser negativo.")
        @Digits(integer = 8, fraction = 2,
                message = "O preço unitário pode ter no máximo 8 algarismos inteiros e 2 casas decimais.")
        BigDecimal precoUnitario,

        String observacoes) {

    public ItemNaoPrevistoCriarPedido {
        observacoes = observacoes == null || observacoes.isBlank() ? null : observacoes.strip();
    }
}
