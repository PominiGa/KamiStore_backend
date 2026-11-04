package com.example.demo.infrastructure.repository;

import com.example.demo.infrastructure.entity.produtos.ItemCarrinho;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemCarrinhoRepository extends JpaRepository<ItemCarrinho, Long> {
}
