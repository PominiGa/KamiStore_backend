package com.example.demo.controller.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record VendedorRequestDTO(
        @Schema(example = "Loja Tech LTDA")
        @NotBlank @Size(max = 100)
        String nome,

        @Schema(example = "vendedor@email.com")
        @NotBlank @Email @Size(max = 100)
        String email,

        @Schema(example = "senha123")
        @NotBlank @Size(min = 6, max = 100)
        String senha,

        @Schema(example = "11.222.333/0001-81", description = "Aceita com ou sem pontuação")
        @NotBlank
        String cnpj,

        List<@Valid EnderecoRequestDTO> enderecos,

        List<@Valid TelefoneRequestDTO> telefones
) {
}
