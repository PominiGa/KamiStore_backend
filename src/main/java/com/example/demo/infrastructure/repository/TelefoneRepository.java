package com.example.demo.infrastructure.repository;

import com.example.demo.infrastructure.entity.usuarios.Telefone;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TelefoneRepository extends JpaRepository<Telefone, Long> {
}
