package com.example.demo.controller.dtos.response;

import com.example.demo.infrastructure.entity.usuarios.Telefone;

public record TelefoneResponseDTO(
        Long id,
        Integer ddd,
        String numero
) {
    public static TelefoneResponseDTO from(Telefone t) {
        return new TelefoneResponseDTO(t.getId(), t.getDdd(), t.getTelefone());
    }
}
