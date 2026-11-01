package com.example.demo.infrastructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.security.InvalidParameterException;
import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "usuario")

public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;
    @Column(name = "senha", nullable = false, length = 10)
    private String senha;
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "usuario_id", referencedColumnName = "id")
    private List<Endereco> enderecos;
    @OneToMany
    @JoinColumn(name = "usuario_id", referencedColumnName = "id")
    private List<Telefone> telefones;

    public void setEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new InvalidParameterException("Email não pode ser vazio");
        }
        if(!email.contains("@") || email.contains(".")) {
            throw new InvalidParameterException("Email invalido");
        }
    }
}
