package com.example.demo.controller;

import com.example.demo.controller.dtos.request.ClienteRequestDTO;
import com.example.demo.controller.dtos.response.ClienteResponseDTO;
import com.example.demo.service.ClienteService;
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
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Cadastro e consulta de clientes (compradores)")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements // endpoint público
    @Operation(summary = "Cadastrar cliente", description = "Cria o usuário com perfil CLIENTE, valida o CPF e cria o carrinho")
    public ResponseEntity<ClienteResponseDTO> cadastrar(@RequestBody @Valid ClienteRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.criarCliente(dto));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('CLIENTE')")
    @Operation(summary = "Meus dados de cliente")
    public ResponseEntity<ClienteResponseDTO> meuPerfil(@AuthenticationPrincipal UserDetails usuarioLogado) {
        return ResponseEntity.ok(clienteService.buscarMeuPerfil(usuarioLogado.getUsername()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "[ADMIN] Listar clientes")
    public ResponseEntity<List<ClienteResponseDTO>> listar() {
        return ResponseEntity.ok(clienteService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "[ADMIN] Buscar cliente por ID")
    public ResponseEntity<ClienteResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(clienteService.buscarPorId(id));
    }
}
