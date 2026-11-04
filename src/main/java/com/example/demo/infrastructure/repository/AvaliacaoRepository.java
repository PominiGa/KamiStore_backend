package com.example.demo.infrastructure.repository;

import com.example.demo.infrastructure.entity.produtos.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao,Long> {
}
