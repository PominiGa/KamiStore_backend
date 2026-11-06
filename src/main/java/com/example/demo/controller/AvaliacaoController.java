package com.example.demo.controller;

import com.example.demo.controller.dtos.request.AvaliacaoRequestDTO;
import com.example.demo.controller.dtos.response.AvaliacaoResponseDTO;
import com.example.demo.controller.dtos.response.AvaliacaoResumoResponseDTO;
import com.example.demo.controller.dtos.response.PageResponseDTO;
import com.example.demo.service.AvaliacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Avaliações", description = "Avaliações de produtos feitas por clientes")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    public AvaliacaoController(AvaliacaoService avaliacaoService) {
        this.avaliacaoService = avaliacaoService;
    }

    @GetMapping("/produtos/{produtoId}/avaliacoes")
    @SecurityRequirements
    @Operation(summary = "Listar avaliações de um produto")
    public ResponseEntity<PageResponseDTO<AvaliacaoResponseDTO>> listar(
            @PathVariable Long produtoId,
            @ParameterObject @PageableDefault(size = 20, sort = "dataAvaliacao", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(avaliacaoService.listarPorProduto(produtoId, pageable));
    }

    @GetMapping("/produtos/{produtoId}/avaliacoes/resumo")
    @SecurityRequirements
    @Operation(summary = "Nota média de um produto")
    public ResponseEntity<AvaliacaoResumoResponseDTO> resumo(@PathVariable Long produtoId) {
        return ResponseEntity.ok(avaliacaoService.resumo(produtoId));
    }

    @PostMapping("/produtos/{produtoId}/avaliacoes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CLIENTE')")
    @Operation(summary = "[CLIENTE] Avaliar produto", description = "Exige um pedido ENTREGUE contendo o produto; uma avaliação por cliente")
    public ResponseEntity<AvaliacaoResponseDTO> criar(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                      @PathVariable Long produtoId,
                                                      @RequestBody @Valid AvaliacaoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(avaliacaoService.criar(usuarioLogado.getUsername(), produtoId, dto));
    }

    @PutMapping("/avaliacoes/{id}")
    @PreAuthorize("hasAuthority('CLIENTE')")
    @Operation(summary = "[CLIENTE] Editar minha avaliação")
    public ResponseEntity<AvaliacaoResponseDTO> atualizar(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                          @PathVariable Long id,
                                                          @RequestBody @Valid AvaliacaoRequestDTO dto) {
        return ResponseEntity.ok(avaliacaoService.atualizar(usuarioLogado.getUsername(), id, dto));
    }

    @DeleteMapping("/avaliacoes/{id}")
    @PreAuthorize("hasAnyAuthority('CLIENTE', 'ADMIN')")
    @Operation(summary = "Excluir avaliação", description = "O autor ou um ADMIN (moderação)")
    public ResponseEntity<Void> deletar(@AuthenticationPrincipal UserDetails usuarioLogado, @PathVariable Long id) {
        avaliacaoService.deletar(usuarioLogado.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
