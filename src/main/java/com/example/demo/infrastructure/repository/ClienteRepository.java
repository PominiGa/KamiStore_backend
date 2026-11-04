package com.example.demo.infrastructure.repository;

import com.example.demo.infrastructure.entity.usuarios.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByCpf(String cpf);
}
