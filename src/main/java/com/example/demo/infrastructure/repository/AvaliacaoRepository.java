package com.example.demo.infrastructure.repository;

import com.example.demo.infrastructure.entity.produtos.Avaliacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    boolean existsByClienteIdAndProdutoId(Long clienteId, Long produtoId);

    Page<Avaliacao> findByProdutoId(Long produtoId, Pageable pageable);

    long countByProdutoId(Long produtoId);

    @Query("select avg(a.nota) from Avaliacao a where a.produto.id = :produtoId")
    Double mediaNotasPorProduto(@Param("produtoId") Long produtoId);
}
