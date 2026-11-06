package com.example.demo.controller.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public record TelefoneRequestDTO(
        @Schema(example = "11")
        @NotNull @Min(11) @Max(99)
        Integer ddd,

        @Schema(example = "987654321")
        @NotBlank @Pattern(regexp = "\\d{8,9}", message = "deve ter 8 ou 9 dígitos")
        String numero
) {
}
