package com.example.demo.controller.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @Schema(example = "cliente@email.com")
        @NotBlank @Email
        String email,

        @Schema(example = "senha123")
        @NotBlank
        String senha
) {
}
