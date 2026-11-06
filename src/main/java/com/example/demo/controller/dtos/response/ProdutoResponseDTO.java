package com.example.demo.controller.dtos.response;

import com.example.demo.infrastructure.entity.enums.ProdutoStatus;
import com.example.demo.infrastructure.entity.produtos.Produto;

import java.math.BigDecimal;

public record ProdutoResponseDTO(
        Long id,
        String nome,
        String descricao,
        String ean,
        BigDecimal preco,
        Integer quantidade,
        ProdutoStatus status,
        Long vendedorId,
        String vendedorNome
) {
    public static ProdutoResponseDTO from(Produto p) {
        return new ProdutoResponseDTO(p.getId(), p.getNome(), p.getDescricao(), p.getEan(), p.getPreco(),
                p.getQuantidade(), p.getProdutoStatus(),
                p.getVendedor().getId(), p.getVendedor().getUsuario().getNome());
    }
}
