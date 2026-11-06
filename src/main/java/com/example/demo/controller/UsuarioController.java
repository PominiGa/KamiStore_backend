package com.example.demo.controller;

import com.example.demo.controller.dtos.request.EnderecoRequestDTO;
import com.example.demo.controller.dtos.request.LoginRequestDTO;
import com.example.demo.controller.dtos.request.TelefoneRequestDTO;
import com.example.demo.controller.dtos.request.UsuarioUpdateRequestDTO;
import com.example.demo.controller.dtos.response.EnderecoResponseDTO;
import com.example.demo.controller.dtos.response.LoginResponseDTO;
import com.example.demo.controller.dtos.response.TelefoneResponseDTO;
import com.example.demo.controller.dtos.response.UsuarioResponseDTO;
import com.example.demo.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuario")
@Tag(name = "Usuário", description = "Login, perfil, endereços e telefones do usuário logado")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    @SecurityRequirements // endpoint público
    @Operation(summary = "Login", description = "Autentica com email e senha e retorna o token JWT")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.login(dto));
    }

    @GetMapping("/me")
    @Operation(summary = "Meu perfil")
    public ResponseEntity<UsuarioResponseDTO> meuPerfil(@AuthenticationPrincipal UserDetails usuarioLogado) {
        return ResponseEntity.ok(usuarioService.buscarMeuPerfil(usuarioLogado.getUsername()));
    }

    @PutMapping("/me")
    @Operation(summary = "Atualizar meu perfil", description = "Altera nome e/ou senha (campos não enviados não são alterados)")
    public ResponseEntity<UsuarioResponseDTO> atualizar(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                        @RequestBody @Valid UsuarioUpdateRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizar(usuarioLogado.getUsername(), dto));
    }

    // ===================== Endereços =====================

    @GetMapping("/me/enderecos")
    @Operation(summary = "Listar meus endereços")
    public ResponseEntity<List<EnderecoResponseDTO>> listarEnderecos(@AuthenticationPrincipal UserDetails usuarioLogado) {
        return ResponseEntity.ok(usuarioService.listarEnderecos(usuarioLogado.getUsername()));
    }

    @PostMapping("/me/enderecos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adicionar endereço")
    public ResponseEntity<EnderecoResponseDTO> adicionarEndereco(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                                 @RequestBody @Valid EnderecoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.adicionarEndereco(usuarioLogado.getUsername(), dto));
    }

    @PutMapping("/me/enderecos/{id}")
    @Operation(summary = "Atualizar endereço")
    public ResponseEntity<EnderecoResponseDTO> atualizarEndereco(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                                 @PathVariable Long id,
                                                                 @RequestBody @Valid EnderecoRequestDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizarEndereco(usuarioLogado.getUsername(), id, dto));
    }

    @DeleteMapping("/me/enderecos/{id}")
    @Operation(summary = "Remover endereço")
    public ResponseEntity<Void> removerEndereco(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                @PathVariable Long id) {
        usuarioService.removerEndereco(usuarioLogado.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    // ===================== Telefones =====================

    @GetMapping("/me/telefones")
    @Operation(summary = "Listar meus telefones")
    public ResponseEntity<List<TelefoneResponseDTO>> listarTelefones(@AuthenticationPrincipal UserDetails usuarioLogado) {
        return ResponseEntity.ok(usuarioService.listarTelefones(usuarioLogado.getUsername()));
    }

    @PostMapping("/me/telefones")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adicionar telefone")
    public ResponseEntity<TelefoneResponseDTO> adicionarTelefone(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                                 @RequestBody @Valid TelefoneRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(usuarioService.adicionarTelefone(usuarioLogado.getUsername(), dto));
    }

    @DeleteMapping("/me/telefones/{id}")
    @Operation(summary = "Remover telefone")
    public ResponseEntity<Void> removerTelefone(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                @PathVariable Long id) {
        usuarioService.removerTelefone(usuarioLogado.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    // ===================== Administração =====================

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "[ADMIN] Listar todos os usuários")
    public ResponseEntity<List<UsuarioResponseDTO>> getAll() {
        return ResponseEntity.ok(usuarioService.listarTodos());
    }

    @GetMapping(params = "email")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "[ADMIN] Buscar usuário por email")
    public ResponseEntity<UsuarioResponseDTO> buscaUsuarioPorEmail(@RequestParam String email) {
        return ResponseEntity.ok(usuarioService.buscarUsuarioPorEmail(email));
    }

    @DeleteMapping(params = "email")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "[ADMIN] Deletar usuário por email")
    public ResponseEntity<Void> deletarUsuarioPorEmail(@RequestParam String email) {
        usuarioService.deletarUsuarioPorEmail(email);
        return ResponseEntity.noContent().build();
    }
}
