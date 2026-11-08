package com.example.demo.infrastructure.entity.produtos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// Variação de um produto (ex.: cor "Azul" + tamanho "M"), com estoque próprio e preço opcional.
// Não é apagada do banco porque pode estar em pedidos: ao ser removida fica com ativo = false
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "produto_variacao")
public class ProdutoVariacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(length = 50)
    private String cor;

    @Column(length = 20)
    private String tamanho;

    // Se null, usa o preço do produto
    @Column(precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(nullable = false)
    private boolean ativo = true;

    public BigDecimal getPrecoEfetivo() {
        return preco != null ? preco : produto.getPreco();
    }

    // Ex.: "Azul / M", "Azul" ou "M"
    public String getDescricao() {
        if (cor != null && tamanho != null) return cor + " / " + tamanho;
        return cor != null ? cor : tamanho;
    }
}
