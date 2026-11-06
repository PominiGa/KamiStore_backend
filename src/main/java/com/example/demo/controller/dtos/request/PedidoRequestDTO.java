package com.example.demo.controller.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record PedidoRequestDTO(
        @Schema(example = "1", description = "ID de um endereço cadastrado do cliente")
        @NotNull
        Long enderecoId
) {
}
