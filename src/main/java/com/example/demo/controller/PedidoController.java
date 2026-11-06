package com.example.demo.controller;

import com.example.demo.controller.dtos.request.PedidoRequestDTO;
import com.example.demo.controller.dtos.request.PedidoStatusRequestDTO;
import com.example.demo.controller.dtos.response.PedidoResponseDTO;
import com.example.demo.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/pedidos")
@Tag(name = "Pedidos", description = "Checkout, acompanhamento e gestão de pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CLIENTE')")
    @Operation(summary = "[CLIENTE] Finalizar compra",
            description = "Gera o pedido a partir do carrinho, baixa o estoque e esvazia o carrinho. Status inicial: AGUARDANDO_PAGAMENTO")
    public ResponseEntity<PedidoResponseDTO> finalizarCompra(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                             @RequestBody @Valid PedidoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pedidoService.criarPedido(usuarioLogado.getUsername(), dto));
    }

    @GetMapping("/meus")
    @PreAuthorize("hasAuthority('CLIENTE')")
    @Operation(summary = "[CLIENTE] Meus pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> meusPedidos(@AuthenticationPrincipal UserDetails usuarioLogado) {
        return ResponseEntity.ok(pedidoService.listarMeusPedidos(usuarioLogado.getUsername()));
    }

    @GetMapping("/vendas")
    @PreAuthorize("hasAuthority('VENDEDOR')")
    @Operation(summary = "[VENDEDOR] Minhas vendas", description = "Pedidos que contêm produtos do vendedor logado")
    public ResponseEntity<List<PedidoResponseDTO>> minhasVendas(@AuthenticationPrincipal UserDetails usuarioLogado) {
        return ResponseEntity.ok(pedidoService.listarVendas(usuarioLogado.getUsername()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "[ADMIN] Listar todos os pedidos")
    public ResponseEntity<List<PedidoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(pedidoService.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhar pedido", description = "Acessível ao cliente dono, aos vendedores dos itens e ao ADMIN")
    public ResponseEntity<PedidoResponseDTO> buscarPorId(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                         @PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.buscarPorId(usuarioLogado.getUsername(), id));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAuthority('CLIENTE')")
    @Operation(summary = "[CLIENTE] Cancelar pedido", description = "Permitido até o pagamento ser aprovado. Devolve os itens ao estoque")
    public ResponseEntity<PedidoResponseDTO> cancelar(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                      @PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.cancelar(usuarioLogado.getUsername(), id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('VENDEDOR', 'ADMIN')")
    @Operation(summary = "[VENDEDOR/ADMIN] Atualizar status",
            description = "Fluxo: AGUARDANDO_PAGAMENTO → PAGAMENTO_APROVADO → EM_PREPARACAO → ENVIADO → ENTREGUE. "
                    + "CANCELADO é permitido antes de EM_PREPARACAO")
    public ResponseEntity<PedidoResponseDTO> atualizarStatus(@AuthenticationPrincipal UserDetails usuarioLogado,
                                                             @PathVariable Long id,
                                                             @RequestBody @Valid PedidoStatusRequestDTO dto) {
        return ResponseEntity.ok(pedidoService.atualizarStatus(usuarioLogado.getUsername(), id, dto.status()));
    }
}
