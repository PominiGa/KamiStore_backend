package com.example.demo.infrastructure.repository;

import com.example.demo.infrastructure.entity.produtos.ProdutoVariacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProdutoVariacaoRepository extends JpaRepository<ProdutoVariacao, Long> {

    List<ProdutoVariacao> findByProdutoIdAndAtivoTrueOrderById(Long produtoId);

    // Variações ativas de vários produtos de uma vez (usado nas listagens paginadas)
    List<ProdutoVariacao> findByProdutoIdInAndAtivoTrueOrderById(Collection<Long> produtoIds);

    Optional<ProdutoVariacao> findByIdAndProdutoIdAndAtivoTrue(Long id, Long produtoId);

    boolean existsByProdutoIdAndAtivoTrue(Long produtoId);
}
