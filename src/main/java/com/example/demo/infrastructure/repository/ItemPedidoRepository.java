package com.example.demo.infrastructure.repository;

import com.example.demo.infrastructure.entity.produtos.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemPedidoRepository extends JpaRepository<ItemPedido,Long> {
}
