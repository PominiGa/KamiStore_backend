package com.example.demo.controller.dtos.response;

import com.example.demo.infrastructure.entity.enums.ProdutoStatus;
import com.example.demo.infrastructure.entity.produtos.Produto;

import java.math.BigDecimal;
import java.util.List;

public record ProdutoResponseDTO(
        Long id,
        String nome,
        String descricao,
        String ean,
        BigDecimal preco,
        Integer quantidade,
        ProdutoStatus status,
        Long vendedorId,
        String vendedorNome,
        List<ImagemResponseDTO> imagens,
        List<VariacaoResponseDTO> variacoes
) {
    public static ProdutoResponseDTO from(Produto p, List<Long> imagemIds, List<VariacaoResponseDTO> variacoes) {
        List<ImagemResponseDTO> imagens = imagemIds.stream()
                .map(imagemId -> ImagemResponseDTO.of(p.getId(), imagemId))
                .toList();
        return new ProdutoResponseDTO(p.getId(), p.getNome(), p.getDescricao(), p.getEan(), p.getPreco(),
                p.getQuantidade(), p.getProdutoStatus(),
                p.getVendedor().getId(), p.getVendedor().getUsuario().getNome(), imagens, variacoes);
    }
}
