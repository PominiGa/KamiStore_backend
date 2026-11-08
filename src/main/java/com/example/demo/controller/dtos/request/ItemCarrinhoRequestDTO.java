package com.example.demo.controller.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemCarrinhoRequestDTO(
        @Schema(example = "1")
        @NotNull
        Long produtoId,

        @Schema(example = "3", description = "Obrigatório se o produto tiver variações (cor/tamanho)")
        Long variacaoId,

        @Schema(example = "2")
        @NotNull @Positive
        Integer quantidade
) {
}
