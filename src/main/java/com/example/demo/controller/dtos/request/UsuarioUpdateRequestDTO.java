package com.example.demo.controller.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

// Campos opcionais: só o que vier preenchido é alterado
public record UsuarioUpdateRequestDTO(
        @Schema(example = "Maria Souza")
        @Size(min = 1, max = 100)
        String nome,

        @Schema(example = "novaSenha123")
        @Size(min = 6, max = 100)
        String senha
) {
}
