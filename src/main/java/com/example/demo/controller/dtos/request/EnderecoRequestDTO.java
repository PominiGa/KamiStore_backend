package com.example.demo.controller.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record EnderecoRequestDTO(
        @Schema(example = "Av. Paulista")
        @NotBlank @Size(max = 255)
        String rua,

        @Schema(example = "1000")
        @NotNull @Positive
        Integer numero,

        @Schema(example = "Apto 12")
        @Size(max = 10)
        String complemento,

        @Schema(example = "São Paulo")
        @NotBlank
        String cidade,

        @Schema(example = "SP", description = "Sigla do estado (UF)")
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "deve ser a sigla com 2 letras")
        String estado,

        @Schema(example = "01310-100")
        @NotBlank @Pattern(regexp = "\\d{5}-?\\d{3}", message = "CEP inválido")
        String cep
) {
}
