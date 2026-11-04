package com.example.demo.infrastructure.entity.produtos;

import com.example.demo.infrastructure.entity.usuarios.Cliente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "carrinho")
public class Carrinho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "cliente_id", nullable = false, unique = true)
    private Cliente cliente;

    @OneToMany(mappedBy = "carrinho",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<ItemCarrinho> itens;
}
