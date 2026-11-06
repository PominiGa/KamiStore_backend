package com.example.demo.infrastructure.repository;

import com.example.demo.infrastructure.entity.usuarios.Vendedor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendedorRepository extends JpaRepository<Vendedor, Long> {
    boolean existsByCnpj(String cnpj);

    Optional<Vendedor> findByUsuarioEmail(String email);
}
