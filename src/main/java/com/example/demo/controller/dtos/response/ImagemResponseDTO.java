package com.example.demo.controller.dtos.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImagemResponseDTO(
        Long id,
        @Schema(description = "Caminho para exibir a imagem (use direto no <img src>, prefixado pela URL da API)",
                example = "/api/produtos/1/imagens/3")
        String url
) {
    public static ImagemResponseDTO of(Long produtoId, Long imagemId) {
        return new ImagemResponseDTO(imagemId, "/api/produtos/" + produtoId + "/imagens/" + imagemId);
    }
}
