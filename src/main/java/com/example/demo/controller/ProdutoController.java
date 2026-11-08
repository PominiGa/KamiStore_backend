package com.example.demo.controller;

import com.example.demo.controller.dtos.request.ProdutoRequestDTO;
import com.example.demo.controller.dtos.request.ProdutoStatusRequestDTO;
import com.example.demo.controller.dtos.response.PageResponseDTO;
import com.example.demo.controller.dtos.response.ProdutoResponseDTO;
import com.example.demo.infrastructure.entity.produtos.ProdutoImagem;
import com.example.demo.service.ProdutoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/produtos")
@Tag(name = "Produtos", description = "Vitrine pública e gestão de produtos pelo vendedor")
public class ProdutoController {

    private final ProdutoService produtoService;

    public ProdutoController(ProdutoService produtoService) {
        this.produtoService = produtoService;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Listar / buscar produtos", description = "Lista produtos ATIVOS, com busca opcional por nome. Ex.: ?nome=rtx&sort=preco,asc")
    public ResponseEntity<PageResponseDTO<ProdutoResponseDTO>> listar(
            @Parameter(description = "Trecho do nome do produto") @RequestParam(required = false) String nome,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(produtoService.listar(nome, pageable));
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Detalhar produto")
    public ResponseEntity<ProdutoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @GetMapping("/vendedor/{vendedorId}")
    @SecurityRequirements
    @Operation(summary = "Produtos de uma loja", description = "Lista os produtos ATIVOS de um vendedor")
    public ResponseEntity<PageResponseDTO<ProdutoResponseDTO>> listarPorVendedor(
            @PathVariable Long vendedorId,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(produtoService.listarPorVendedor(vendedorId, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('VENDEDOR')")
    @Operation(summary = "[VENDEDOR] Cadastrar produto")
    public ResponseEntity<ProdutoResponseDTO> criar(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                    @RequestBody @Valid ProdutoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.criar(usuarioLogado.getUsername(), dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('VENDEDOR', 'ADMIN')")
    @Operation(summary = "[VENDEDOR] Atualizar produto", description = "Apenas o vendedor dono do produto (ou ADMIN)")
    public ResponseEntity<ProdutoResponseDTO> atualizar(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                        @PathVariable Long id,
                                                        @RequestBody @Valid ProdutoRequestDTO dto) {
        return ResponseEntity.ok(produtoService.atualizar(usuarioLogado.getUsername(), id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('VENDEDOR', 'ADMIN')")
    @Operation(summary = "[VENDEDOR] Ativar / inativar produto")
    public ResponseEntity<ProdutoResponseDTO> alterarStatus(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                            @PathVariable Long id,
                                                            @RequestBody @Valid ProdutoStatusRequestDTO dto) {
        return ResponseEntity.ok(produtoService.alterarStatus(usuarioLogado.getUsername(), id, dto.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('VENDEDOR', 'ADMIN')")
    @Operation(summary = "[VENDEDOR] Remover produto", description = "Remoção lógica: o produto fica INATIVO (pode existir em pedidos)")
    public ResponseEntity<Void> remover(@AuthenticationPrincipal UserDetails usuarioLogado, @PathVariable Long id) {
        produtoService.desativar(usuarioLogado.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    // ===================== Imagens =====================

    @PostMapping(value = "/{id}/imagens", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('VENDEDOR', 'ADMIN')")
    @Operation(summary = "[VENDEDOR] Enviar imagens do produto",
            description = "multipart/form-data com uma ou mais imagens no campo 'imagens' (JPEG, PNG, WEBP ou GIF, até 5MB cada, máx. 10 por produto)")
    public ResponseEntity<ProdutoResponseDTO> adicionarImagens(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                               @PathVariable Long id,
                                                               @RequestParam("imagens") List<MultipartFile> imagens) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(produtoService.adicionarImagens(usuarioLogado.getUsername(), id, imagens));
    }

    @GetMapping("/{id}/imagens/{imagemId}")
    @SecurityRequirements
    @Operation(summary = "Exibir imagem do produto", description = "Retorna o arquivo da imagem (use no <img src>)")
    public ResponseEntity<byte[]> buscarImagem(@PathVariable Long id, @PathVariable Long imagemId) {
        ProdutoImagem imagem = produtoService.buscarImagem(id, imagemId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagem.getContentType()))
                .contentLength(imagem.getDados().length)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(imagem.getNomeArquivo(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(imagem.getDados());
    }

    @DeleteMapping("/{id}/imagens/{imagemId}")
    @PreAuthorize("hasAnyAuthority('VENDEDOR', 'ADMIN')")
    @Operation(summary = "[VENDEDOR] Remover imagem do produto")
    public ResponseEntity<Void> removerImagem(@AuthenticationPrincipal UserDetails usuarioLogado,
                                              @PathVariable Long id,
                                              @PathVariable Long imagemId) {
        produtoService.removerImagem(usuarioLogado.getUsername(), id, imagemId);
        return ResponseEntity.noContent().build();
    }
}
