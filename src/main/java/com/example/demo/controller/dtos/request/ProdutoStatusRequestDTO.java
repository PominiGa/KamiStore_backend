package com.example.demo.controller.dtos.request;

import com.example.demo.infrastructure.entity.enums.ProdutoStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ProdutoStatusRequestDTO(
        @Schema(example = "INATIVO", description = "Apenas ATIVO ou INATIVO podem ser definidos manualmente")
        @NotNull
        ProdutoStatus status
) {
}
