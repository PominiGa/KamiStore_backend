package com.example.demo.infrastructure.entity.usuarios;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "endereco")
public class Endereco {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "rua")
    private String rua;
    @Column(name = "numero")
    private int numero;
    @Column(name = "Complemento", length = 10)
    private String complemento;
    @Column(name = "cidade")
    private String cidade;
    @Column(name = "estado_sigla", length = 2)
    private String estado;
    @Column(name = "cep", length = 9)
    private String cep;
}
