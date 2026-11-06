package com.example.demo.controller;

import com.example.demo.controller.dtos.request.ItemCarrinhoRequestDTO;
import com.example.demo.controller.dtos.request.QuantidadeRequestDTO;
import com.example.demo.controller.dtos.response.CarrinhoResponseDTO;
import com.example.demo.service.CarrinhoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrinho")
@PreAuthorize("hasAuthority('CLIENTE')")
@Tag(name = "Carrinho", description = "Carrinho de compras do cliente logado")
public class CarrinhoController {

    private final CarrinhoService carrinhoService;

    public CarrinhoController(CarrinhoService carrinhoService) {
        this.carrinhoService = carrinhoService;
    }

    @GetMapping
    @Operation(summary = "Ver meu carrinho", description = "Itens, quantidade total e valor total")
    public ResponseEntity<CarrinhoResponseDTO> buscar(@AuthenticationPrincipal UserDetails usuarioLogado) {
        return ResponseEntity.ok(carrinhoService.buscar(usuarioLogado.getUsername()));
    }

    @PostMapping("/itens")
    @Operation(summary = "Adicionar produto", description = "Se o produto já estiver no carrinho, soma a quantidade")
    public ResponseEntity<CarrinhoResponseDTO> adicionarItem(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                             @RequestBody @Valid ItemCarrinhoRequestDTO dto) {
        return ResponseEntity.ok(carrinhoService.adicionarItem(usuarioLogado.getUsername(), dto));
    }

    @PutMapping("/itens/{produtoId}")
    @Operation(summary = "Alterar quantidade de um produto")
    public ResponseEntity<CarrinhoResponseDTO> atualizarQuantidade(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                                   @PathVariable Long produtoId,
                                                                   @RequestBody @Valid QuantidadeRequestDTO dto) {
        return ResponseEntity.ok(carrinhoService.atualizarQuantidade(usuarioLogado.getUsername(), produtoId, dto.quantidade()));
    }

    @DeleteMapping("/itens/{produtoId}")
    @Operation(summary = "Remover produto do carrinho")
    public ResponseEntity<CarrinhoResponseDTO> removerItem(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                           @PathVariable Long produtoId) {
        return ResponseEntity.ok(carrinhoService.removerItem(usuarioLogado.getUsername(), produtoId));
    }

    @DeleteMapping
    @Operation(summary = "Esvaziar carrinho")
    public ResponseEntity<Void> limpar(@AuthenticationPrincipal UserDetails usuarioLogado) {
        carrinhoService.limpar(usuarioLogado.getUsername());
        return ResponseEntity.noContent().build();
    }
}
