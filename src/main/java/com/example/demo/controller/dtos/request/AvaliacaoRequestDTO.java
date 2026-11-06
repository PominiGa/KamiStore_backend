package com.example.demo.controller.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AvaliacaoRequestDTO(
        @Schema(example = "5", description = "Nota de 1 a 5")
        @NotNull @Min(1) @Max(5)
        Integer nota,

        @Schema(example = "Produto excelente, chegou antes do prazo!")
        @Size(max = 1000)
        String comentario
) {
}
