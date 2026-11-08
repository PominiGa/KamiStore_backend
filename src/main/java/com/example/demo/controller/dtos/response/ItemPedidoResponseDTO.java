package com.example.demo.controller.dtos.response;

import com.example.demo.infrastructure.entity.produtos.ItemPedido;
import com.example.demo.infrastructure.entity.produtos.ProdutoVariacao;

import java.math.BigDecimal;

public record ItemPedidoResponseDTO(
        Long produtoId,
        String nomeProduto,
        Long variacaoId,
        String cor,
        String tamanho,
        Long vendedorId,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal subtotal
) {
    public static ItemPedidoResponseDTO from(ItemPedido i) {
        ProdutoVariacao v = i.getVariacao();
        return new ItemPedidoResponseDTO(i.getProduto().getId(), i.getProduto().getNome(),
                v != null ? v.getId() : null, v != null ? v.getCor() : null, v != null ? v.getTamanho() : null,
                i.getProduto().getVendedor().getId(), i.getQuantidade(), i.getPrecoUnitario(),
                i.getPrecoUnitario().multiply(BigDecimal.valueOf(i.getQuantidade())));
    }
}
