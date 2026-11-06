package com.example.demo.controller.dtos.response;

import com.example.demo.infrastructure.entity.enums.UsuarioRole;
import com.example.demo.infrastructure.entity.usuarios.Usuario;

import java.util.List;

// Nunca expõe a senha
public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        UsuarioRole role,
        List<EnderecoResponseDTO> enderecos,
        List<TelefoneResponseDTO> telefones
) {
    public static UsuarioResponseDTO from(Usuario u) {
        List<EnderecoResponseDTO> enderecos = u.getEnderecos() == null ? List.of()
                : u.getEnderecos().stream().map(EnderecoResponseDTO::from).toList();
        List<TelefoneResponseDTO> telefones = u.getTelefones() == null ? List.of()
                : u.getTelefones().stream().map(TelefoneResponseDTO::from).toList();
        return new UsuarioResponseDTO(u.getId(), u.getNome(), u.getEmail(), u.getUsuarioRole(), enderecos, telefones);
    }
}
