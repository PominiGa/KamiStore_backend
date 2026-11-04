package com.example.demo.infrastructure.repository;

import com.example.demo.infrastructure.entity.produtos.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}
