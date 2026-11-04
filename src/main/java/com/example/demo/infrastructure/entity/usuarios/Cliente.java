package com.example.demo.infrastructure.entity.usuarios;

import com.example.demo.infrastructure.entity.produtos.Avaliacao;
import com.example.demo.infrastructure.entity.produtos.Carrinho;
import com.example.demo.infrastructure.entity.produtos.Pedido;
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
@Table(name = "cliente")
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @OneToOne(mappedBy = "cliente", cascade = CascadeType.ALL,
            orphanRemoval = true)
    private Carrinho carrinho;

    @OneToMany(mappedBy = "cliente")
    private List<Pedido> pedidos;

    @OneToMany(mappedBy = "cliente")
    private List<Avaliacao> avaliacoes;
}
