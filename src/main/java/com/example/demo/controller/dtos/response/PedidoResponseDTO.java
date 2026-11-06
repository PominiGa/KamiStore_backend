package com.example.demo.controller.dtos.response;

import com.example.demo.infrastructure.entity.enums.PedidoStatus;
import com.example.demo.infrastructure.entity.produtos.Pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponseDTO(
        Long id,
        Long clienteId,
        String clienteNome,
        PedidoStatus status,
        LocalDateTime dataPedido,
        String enderecoEntrega,
        BigDecimal valorTotal,
        List<ItemPedidoResponseDTO> itens
) {
    public static PedidoResponseDTO from(Pedido p) {
        return new PedidoResponseDTO(p.getId(), p.getCliente().getId(), p.getCliente().getUsuario().getNome(),
                p.getStatus(), p.getDataPedido(), p.getEnderecoEntrega(), p.getValorTotal(),
                p.getItens().stream().map(ItemPedidoResponseDTO::from).toList());
    }
}
