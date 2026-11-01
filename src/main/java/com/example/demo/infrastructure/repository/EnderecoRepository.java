package com.example.demo.infrastructure.repository;

import com.example.demo.infrastructure.entity.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnderecoRepository extends JpaRepository<Endereco, Long> {
}
