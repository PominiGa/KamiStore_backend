package com.example.demo.controller.dtos.response;

import com.example.demo.infrastructure.entity.produtos.Avaliacao;

import java.time.LocalDateTime;

public record AvaliacaoResponseDTO(
        Long id,
        Long produtoId,
        String clienteNome,
        Integer nota,
        String comentario,
        LocalDateTime dataAvaliacao
) {
    public static AvaliacaoResponseDTO from(Avaliacao a) {
        return new AvaliacaoResponseDTO(a.getId(), a.getProduto().getId(), a.getCliente().getUsuario().getNome(),
                a.getNota(), a.getComentario(), a.getDataAvaliacao());
    }
}
