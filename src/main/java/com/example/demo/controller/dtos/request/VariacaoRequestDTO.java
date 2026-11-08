package com.example.demo.controller.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record VariacaoRequestDTO(
        @Schema(example = "1", description = "Informe ao ATUALIZAR uma variação existente; deixe vazio para criar uma nova")
        Long id,

        @Schema(example = "Azul")
        @Size(max = 50)
        String cor,

        @Schema(example = "M")
        @Size(max = 20)
        String tamanho,

        @Schema(example = "89.90", description = "Opcional: se vazio, usa o preço do produto")
        @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2)
        BigDecimal preco,

        @Schema(example = "5", description = "Estoque desta variação")
        @NotNull @PositiveOrZero
        Integer quantidade
) {
}
