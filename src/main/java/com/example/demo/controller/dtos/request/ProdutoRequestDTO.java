package com.example.demo.controller.dtos.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

public record ProdutoRequestDTO(
        @Schema(example = "Placa de Vídeo RTX 5070 12GB")
        @NotBlank @Size(max = 100)
        String nome,

        @Schema(example = "GDDR7, 12GB, PCIe 5.0")
        @Size(max = 255)
        String descricao,

        @Schema(example = "7891234567895", description = "Código de barras (EAN/GTIN)")
        @NotBlank @Pattern(regexp = "\\d{8,17}", message = "deve conter de 8 a 17 dígitos")
        String ean,

        @Schema(example = "3999.90")
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2)
        BigDecimal preco,

        @Schema(example = "10", description = "Estoque do produto. Obrigatório se não houver variações; "
                + "com variações, o estoque é a soma das variações e este campo é ignorado")
        @PositiveOrZero
        Integer quantidade,

        @Schema(description = "Cores/tamanhos do produto. Omitir o campo mantém as variações atuais; "
                + "enviar uma lista substitui (as que não vierem são removidas); [] remove todas")
        List<@Valid VariacaoRequestDTO> variacoes
) {
}
