package com.example.demo.controller.dtos.response;

import com.example.demo.infrastructure.entity.produtos.ItemPedido;

import java.math.BigDecimal;

public record ItemPedidoResponseDTO(
        Long produtoId,
        String nomeProduto,
        Long vendedorId,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {
    public static ItemPedidoResponseDTO from(ItemPedido i) {
        return new ItemPedidoResponseDTO(i.getProduto().getId(), i.getProduto().getNome(),
                i.getProduto().getVendedor().getId(), i.getQuantidade(), i.getPrecoUnitario(),
                i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())));
    }
}
