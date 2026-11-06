package com.example.demo.controller.dtos.response;

import com.example.demo.infrastructure.entity.usuarios.Endereco;

public record EnderecoResponseDTO(
        Long id,
        String rua,
        Integer numero,
        String complemento,
        String cidade,
        String estado,
        String cep
) {
    public static EnderecoResponseDTO from(Endereco e) {
        return new EnderecoResponseDTO(e.getId(), e.getRua(), e.getNumero(), e.getComplemento(),
                e.getCidade(), e.getEstado(), e.getCep());
    }
}
