package com.example.demo.controller.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ClienteRequestDTO(
        @Schema(example = "Maria Silva")
        @NotBlank @Size(max = 100)
        String nome,

        @Schema(example = "cliente@email.com")
        @NotBlank @Email @Size(max = 100)
        String email,

        @Schema(example = "senha123")
        @NotBlank @Size(min = 6, max = 100)
        String senha,

        @Schema(example = "529.982.247-25", description = "Aceita com ou sem pontuação")
        @NotBlank
        String cpf,

        List<@Valid EnderecoRequestDTO> enderecos,

        List<@Valid TelefoneRequestDTO> telefones
) {
}
