package com.example.demo.infrastructure.repository;

import com.example.demo.infrastructure.entity.produtos.ProdutoImagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProdutoImagemRepository extends JpaRepository<ProdutoImagem, Long> {

    // Apenas (produtoId, imagemId): evita carregar os bytes das imagens ao listar produtos
    @Query("""
            select new com.example.demo.infrastructure.repository.ImagemRef(i.produto.id, i.id)
            from ProdutoImagem i
            where i.produto.id in :produtoIds
            order by i.id
            """)
    List<ImagemRef> findRefsByProdutoIds(@Param("produtoIds") Collection<Long> produtoIds);

    Optional<ProdutoImagem> findByIdAndProdutoId(Long id, Long produtoId);

    long countByProdutoId(Long produtoId);

    @Modifying
    @Query("delete from ProdutoImagem i where i.id = :id and i.produto.id = :produtoId")
    int deleteByIdAndProdutoId(@Param("id") Long id, @Param("produtoId") Long produtoId);
}
