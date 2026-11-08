package com.example.demo.controller.dtos.response;

import com.example.demo.infrastructure.entity.produtos.ItemCarrinho;
import com.example.demo.infrastructure.entity.produtos.ProdutoVariacao;

import java.math.BigDecimal;

public record ItemCarrinhoResponseDTO(
        Long produtoId,
        String nomeProduto,
        Long variacaoId,
        String cor,
        String tamanho,
        BigDecimal precoUnitario,
        Integer quantidade,
        BigDecimal subtotal
) {
    public static ItemCarrinhoResponseDTO from(ItemCarrinho i) {
        ProdutoVariacao v = i.getVariacao();
        BigDecimal preco = v != null ? v.getPrecoEfetivo() : i.getProduto().getPreco();
        return new ItemCarrinhoResponseDTO(i.getProduto().getId(), i.getProduto().getNome(),
                v != null ? v.getId() : null, v != null ? v.getCor() : null, v != null ? v.getTamanho() : null,
                preco,
                i.getQuantidade(), preco.multiply(BigDecimal.valueOf(i.getQuantidade())));
    }
}
