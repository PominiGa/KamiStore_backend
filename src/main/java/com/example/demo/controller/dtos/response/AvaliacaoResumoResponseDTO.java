package com.example.demo.controller.dtos.response;

public record AvaliacaoResumoResponseDTO(
        Long produtoId,
        Double mediaNotas,
        Long totalAvaliacoes
) {
}
