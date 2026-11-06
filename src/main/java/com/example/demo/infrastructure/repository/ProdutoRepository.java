package com.example.demo.infrastructure.repository;

import com.example.demo.infrastructure.entity.enums.ProdutoStatus;
import com.example.demo.infrastructure.entity.produtos.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    boolean existsByEan(String ean);

    boolean existsByEanAndIdNot(String ean, Long id);

    Page<Produto> findByProdutoStatus(ProdutoStatus status, Pageable pageable);

    Page<Produto> findByNomeContainingIgnoreCaseAndProdutoStatus(String nome, ProdutoStatus status, Pageable pageable);

    Page<Produto> findByVendedorIdAndProdutoStatus(Long vendedorId, ProdutoStatus status, Pageable pageable);

    Page<Produto> findByVendedorId(Long vendedorId, Pageable pageable);
}
