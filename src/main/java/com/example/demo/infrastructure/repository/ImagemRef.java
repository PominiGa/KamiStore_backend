package com.example.demo.infrastructure.repository;

// Projeção leve de uma imagem de produto (sem os bytes)
public record ImagemRef(Long produtoId, Long imagemId) {
}
