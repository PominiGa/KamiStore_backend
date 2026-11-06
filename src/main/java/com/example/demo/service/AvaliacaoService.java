package com.example.demo.service;

import com.example.demo.controller.dtos.request.AvaliacaoRequestDTO;
import com.example.demo.controller.dtos.response.AvaliacaoResponseDTO;
import com.example.demo.controller.dtos.response.AvaliacaoResumoResponseDTO;
import com.example.demo.controller.dtos.response.PageResponseDTO;
import com.example.demo.infrastructure.entity.enums.PedidoStatus;
import com.example.demo.infrastructure.entity.produtos.Avaliacao;
import com.example.demo.infrastructure.entity.produtos.Produto;
import com.example.demo.infrastructure.entity.usuarios.Cliente;
import com.example.demo.infrastructure.exceptions.ConflictException;
import com.example.demo.infrastructure.exceptions.RegraNegocioException;
import com.example.demo.infrastructure.exceptions.ResourceNotFoundException;
import com.example.demo.infrastructure.repository.AvaliacaoRepository;
import com.example.demo.infrastructure.repository.PedidoRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final PedidoRepository pedidoRepository;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final UsuarioService usuarioService;

    public AvaliacaoService(AvaliacaoRepository avaliacaoRepository,
                            PedidoRepository pedidoRepository,
                            ClienteService clienteService,
                            ProdutoService produtoService,
                            UsuarioService usuarioService) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.pedidoRepository = pedidoRepository;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
        this.usuarioService = usuarioService;
    }

    // Só quem recebeu o produto (pedido ENTREGUE) pode avaliar, e apenas uma vez
    @Transactional
    public AvaliacaoResponseDTO criar(String email, Long produtoId, AvaliacaoRequestDTO dto) {
        Cliente cliente = clienteService.buscarEntidadePorEmail(email);
        Produto produto = produtoService.buscarEntidadePorId(produtoId);

        if (!pedidoRepository.clienteComprouProduto(cliente.getId(), produtoId, PedidoStatus.ENTREGUE)) {
            throw new RegraNegocioException("Você só pode avaliar produtos de pedidos já entregues");
        }
        if (avaliacaoRepository.existsByClienteIdAndProdutoId(cliente.getId(), produtoId)) {
            throw new ConflictException("Você já avaliou este produto");
        }

        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setCliente(cliente);
        avaliacao.setProduto(produto);
        avaliacao.setNota(dto.nota());
        avaliacao.setComentario(dto.comentario());
        avaliacao.setDataAvaliacao(LocalDateTime.now());

        return AvaliacaoResponseDTO.from(avaliacaoRepository.save(avaliacao));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<AvaliacaoResponseDTO> listarPorProduto(Long produtoId, Pageable pageable) {
        produtoService.buscarEntidadePorId(produtoId);
        return PageResponseDTO.from(avaliacaoRepository.findByProdutoId(produtoId, pageable)
                .map(AvaliacaoResponseDTO::from));
    }

    @Transactional(readOnly = true)
    public AvaliacaoResumoResponseDTO resumo(Long produtoId) {
        produtoService.buscarEntidadePorId(produtoId);
        Double media = avaliacaoRepository.mediaNotasPorProduto(produtoId);
        long total = avaliacaoRepository.countByProdutoId(produtoId);
        Double mediaArredondada = media == null ? 0.0 : Math.round(media * 10) / 10.0;
        return new AvaliacaoResumoResponseDTO(produtoId, mediaArredondada, total);
    }

    @Transactional
    public AvaliacaoResponseDTO atualizar(String email, Long id, AvaliacaoRequestDTO dto) {
        Avaliacao avaliacao = buscarEntidadePorId(id);
        if (!avaliacao.getCliente().getUsuario().getEmail().equals(email)) {
            throw new AccessDeniedException("Esta avaliação não pertence a você");
        }
        avaliacao.setNota(dto.nota());
        avaliacao.setComentario(dto.comentario());
        avaliacao.setDataAvaliacao(LocalDateTime.now());
        return AvaliacaoResponseDTO.from(avaliacaoRepository.save(avaliacao));
    }

    @Transactional
    public void deletar(String email, Long id) {
        Avaliacao avaliacao = buscarEntidadePorId(id);
        boolean dono = avaliacao.getCliente().getUsuario().getEmail().equals(email);
        if (!dono && !usuarioService.isAdmin(email)) {
            throw new AccessDeniedException("Esta avaliação não pertence a você");
        }
        avaliacaoRepository.delete(avaliacao);
    }

    private Avaliacao buscarEntidadePorId(Long id) {
        return avaliacaoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada: " + id));
    }
}
