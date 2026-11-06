package com.example.demo.controller;

import com.example.demo.controller.dtos.request.VendedorRequestDTO;
import com.example.demo.controller.dtos.response.PageResponseDTO;
import com.example.demo.controller.dtos.response.ProdutoResponseDTO;
import com.example.demo.controller.dtos.response.VendedorResponseDTO;
import com.example.demo.service.ProdutoService;
import com.example.demo.service.VendedorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendedores")
@Tag(name = "Vendedores", description = "Cadastro e consulta de vendedores (lojas)")
public class VendedorController {

    private final VendedorService vendedorService;
    private final ProdutoService produtoService;

    public VendedorController(VendedorService vendedorService, ProdutoService produtoService) {
        this.vendedorService = vendedorService;
        this.produtoService = produtoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements // endpoint público
    @Operation(summary = "Cadastrar vendedor", description = "Cria o usuário com perfil VENDEDOR e valida o CNPJ")
    public ResponseEntity<VendedorResponseDTO> cadastrar(@RequestBody @Valid VendedorRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vendedorService.criarVendedor(dto));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('VENDEDOR')")
    @Operation(summary = "Meus dados de vendedor")
    public ResponseEntity<VendedorResponseDTO> meuPerfil(@AuthenticationPrincipal UserDetails usuarioLogado) {
        return ResponseEntity.ok(vendedorService.buscarMeuPerfil(usuarioLogado.getUsername()));
    }

    @GetMapping("/me/produtos")
    @PreAuthorize("hasAuthority('VENDEDOR')")
    @Operation(summary = "Meus produtos", description = "Todos os produtos do vendedor logado, em qualquer status")
    public ResponseEntity<PageResponseDTO<ProdutoResponseDTO>> meusProdutos(
            @AuthenticationPrincipal UserDetails usuarioLogado,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(produtoService.listarMeusProdutos(usuarioLogado.getUsername(), pageable));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "[ADMIN] Listar vendedores")
    public ResponseEntity<List<VendedorResponseDTO>> listar() {
        return ResponseEntity.ok(vendedorService.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "[ADMIN] Buscar vendedor por ID")
    public ResponseEntity<VendedorResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(vendedorService.buscarPorId(id));
    }
}
