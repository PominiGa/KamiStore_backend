package com.example.demo.controller.dtos.response;

import com.example.demo.infrastructure.entity.produtos.Carrinho;

import java.math.BigDecimal;
import java.util.List;

public record CarrinhoResponseDTO(
        Long id,
        List<ItemCarrinhoResponseDTO> itens,
        Integer quantidadeTotal,
        BigDecimal valorTotal
) {
    public static CarrinhoResponseDTO from(Carrinho c) {
        List<ItemCarrinhoResponseDTO> itens = c.getItens().stream().map(ItemCarrinhoResponseDTO::from).toList();
        int quantidade = itens.stream().mapToInt(ItemCarrinhoResponseDTO::quantidade).sum();
        BigDecimal total = itens.stream().map(ItemCarrinhoResponseDTO::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CarrinhoResponseDTO(c.getId(), itens, quantidade, total);
    }
}
