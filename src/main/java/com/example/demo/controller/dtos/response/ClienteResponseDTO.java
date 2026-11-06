package com.example.demo.controller.dtos.response;

import com.example.demo.infrastructure.entity.usuarios.Cliente;

public record ClienteResponseDTO(
        Long id,
        String cpf,
        UsuarioResponseDTO usuario
) {
    public static ClienteResponseDTO from(Cliente c) {
        return new ClienteResponseDTO(c.getId(), c.getCpf(), UsuarioResponseDTO.from(c.getUsuario()));
    }
}
