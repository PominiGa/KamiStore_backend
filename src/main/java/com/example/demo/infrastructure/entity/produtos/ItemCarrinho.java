package com.example.demo.infrastructure.entity.produtos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "item_carrinho",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_item_carrinho_produto_variacao",
                        columnNames = {"carrinho_id", "produto_id", "variacao_id"}
                )
        }
)
public class ItemCarrinho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrinho_id", nullable = false)
    private Carrinho carrinho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    // Cor/tamanho escolhido (null se o produto não tiver variações)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variacao_id")
    private ProdutoVariacao variacao;

    @Column(nullable = false)
    private Integer quantidade;
}