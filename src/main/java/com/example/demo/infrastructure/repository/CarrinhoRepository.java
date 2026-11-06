package com.example.demo.infrastructure.repository;

import com.example.demo.infrastructure.entity.produtos.Carrinho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarrinhoRepository extends JpaRepository<Carrinho, Long> {
    Optional<Carrinho> findByClienteId(Long clienteId);
}
