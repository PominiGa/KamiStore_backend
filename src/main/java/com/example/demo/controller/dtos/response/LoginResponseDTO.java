package com.example.demo.controller.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponseDTO(
        @Schema(description = "Token JWT (cole no botão Authorize do Swagger)")
        String token,
        @Schema(example = "Bearer")
        String tipo
) {
}
