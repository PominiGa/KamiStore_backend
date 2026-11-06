package com.example.demo.controller.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record QuantidadeRequestDTO(
        @Schema(example = "3")
        @NotNull @Positive
        Integer quantidade
) {
}
