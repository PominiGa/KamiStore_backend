package com.example.demo.controller.dtos.response;

import org.springframework.data.domain.Page;

import java.util.List;

// Formato estável de paginação para a API (evita expor o Page do Spring diretamente)
public record PageResponseDTO<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean ultima
) {
    public static <T> PageResponseDTO<T> from(Page<T> page) {
        return new PageResponseDTO<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }
}
