package com.example.demo.infrastructure.entity.produtos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Foto de um produto, salva diretamente no banco (coluna bytea no PostgreSQL)
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "produto_imagem")
public class ProdutoImagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private String nomeArquivo;

    @Column(nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false)
    private Long tamanho;

    // Bytes da imagem. byte[] sem @Lob vira "bytea" no PostgreSQL
    @Column(nullable = false)
    private byte[] dados;
}
