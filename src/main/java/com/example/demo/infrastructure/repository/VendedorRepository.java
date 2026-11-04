package com.example.demo.infrastructure.repository;

import com.example.demo.infrastructure.entity.usuarios.Vendedor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendedorRepository extends JpaRepository<Vendedor, Long> {
}
