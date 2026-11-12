package pt.istec.ishopping.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrcamentoAtualizarPedido(
        @NotNull(message = "O ano é obrigatório.")
        @Min(value = 2000, message = "O ano tem de estar entre 2000 e 2100.")
        @Max(value = 2100, message = "O ano tem de estar entre 2000 e 2100.")
        Integer ano,

        @NotNull(message = "O mês é obrigatório.")
        @Min(value = 1, message = "O mês tem de estar entre 1 e 12.")
        @Max(value = 12, message = "O mês tem de estar entre 1 e 12.")
        Integer mes,

        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0", message = "O valor não pode ser negativo.")
        @Digits(integer = 8, fraction = 2,
                message = "O valor pode ter no máximo 8 algarismos inteiros e 2 casas decimais.")
        BigDecimal valor) {
}
