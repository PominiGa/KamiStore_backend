package com.example.demo.controller.dtos.response;

import com.example.demo.infrastructure.entity.produtos.ProdutoVariacao;

import java.math.BigDecimal;

public record VariacaoResponseDTO(
        Long id,
        String cor,
        String tamanho,
        BigDecimal preco,
        Integer quantidade
) {
    public static VariacaoResponseDTO from(ProdutoVariacao v) {
        return new VariacaoResponseDTO(v.getId(), v.getCor(), v.getTamanho(), v.getPrecoEfetivo(), v.getQuantidade());
    }
}
