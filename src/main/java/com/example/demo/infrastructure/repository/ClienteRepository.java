package com.example.demo.infrastructure.repository;

import com.example.demo.infrastructure.entity.usuarios.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByCpf(String cpf);

    Optional<Cliente> findByUsuarioEmail(String email);
}
