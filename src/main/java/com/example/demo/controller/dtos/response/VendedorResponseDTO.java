package com.example.demo.controller.dtos.response;

import com.example.demo.infrastructure.entity.usuarios.Vendedor;

public record VendedorResponseDTO(
        Long id,
        String cnpj,
        UsuarioResponseDTO usuario
) {
    public static VendedorResponseDTO from(Vendedor v) {
        return new VendedorResponseDTO(v.getId(), v.getCnpj(), UsuarioResponseDTO.from(v.getUsuario()));
    }
}
