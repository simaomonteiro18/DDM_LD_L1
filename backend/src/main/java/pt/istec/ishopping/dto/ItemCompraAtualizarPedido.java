package pt.istec.ishopping.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemCompraAtualizarPedido(
        @NotNull(message = "O artigo é obrigatório.")
        Integer idArtigo,

        @Positive(message = "A quantidade prevista tem de ser maior que zero.")
        Integer quantidadePrevista,

        @Positive(message = "A quantidade adquirida tem de ser maior que zero.")
        Integer quantidadeAdquirida,

        @DecimalMin(value = "0", message = "O preço unitário não pode ser negativo.")
        @Digits(integer = 8, fraction = 2,
                message = "O preço unitário pode ter no máximo 8 algarismos inteiros e 2 casas decimais.")
        BigDecimal precoUnitario,

        String observacoes) {

    public ItemCompraAtualizarPedido {
        observacoes = observacoes == null || observacoes.isBlank() ? null : observacoes.strip();
    }

    @AssertTrue(message = "A quantidade adquirida e o preço unitário têm de ser indicados em conjunto.")
    public boolean isAquisicaoCompleta() {
        return (quantidadeAdquirida == null) == (precoUnitario == null);
    }
}
