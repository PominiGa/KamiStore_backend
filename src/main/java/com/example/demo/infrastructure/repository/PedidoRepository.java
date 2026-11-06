package com.example.demo.infrastructure.repository;

import com.example.demo.infrastructure.entity.enums.PedidoStatus;
import com.example.demo.infrastructure.entity.produtos.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteIdOrderByDataPedidoDesc(Long clienteId);

    @Query("""
            select distinct p from Pedido p
            join p.itens i
            where i.produto.vendedor.id = :vendedorId
            order by p.dataPedido desc
            """)
    List<Pedido> findPedidosComProdutosDoVendedor(@Param("vendedorId") Long vendedorId);

    @Query("""
            select count(p) > 0 from Pedido p
            join p.itens i
            where p.cliente.id = :clienteId
              and i.produto.id = :produtoId
              and p.status = :status
            """)
    boolean clienteComprouProduto(@Param("clienteId") Long clienteId,
                                  @Param("produtoId") Long produtoId,
                                  @Param("status") PedidoStatus status);
}
