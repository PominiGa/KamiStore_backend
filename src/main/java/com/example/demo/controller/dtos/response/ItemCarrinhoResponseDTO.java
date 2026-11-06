package com.example.demo.controller.dtos.response;

import com.example.demo.infrastructure.entity.produtos.ItemCarrinho;

import java.math.BigDecimal;

public record ItemCarrinhoResponseDTO(
        Long produtoId,
        String nomeProduto,
        BigDecimal precoUnitario,
        Integer quantidade,
        BigDecimal subtotal
) {
    public static ItemCarrinhoResponseDTO from(ItemCarrinho i) {
        BigDecimal preco = i.getProduto().getPreco();
        return new ItemCarrinhoResponseDTO(i.getProduto().getId(), i.getProduto().getNome(), preco,
                i.getQuantidade(), preco.multiply(BigDecimal.valueOf(i.getQuantidade())));
    }
}
