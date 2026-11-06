package com.example.demo.service;

import com.example.demo.controller.dtos.request.ProdutoRequestDTO;
import com.example.demo.controller.dtos.response.PageResponseDTO;
import com.example.demo.controller.dtos.response.ProdutoResponseDTO;
import com.example.demo.infrastructure.entity.enums.ProdutoStatus;
import com.example.demo.infrastructure.entity.produtos.Produto;
import com.example.demo.infrastructure.entity.usuarios.Vendedor;
import com.example.demo.infrastructure.exceptions.ConflictException;
import com.example.demo.infrastructure.exceptions.RegraNegocioException;
import com.example.demo.infrastructure.exceptions.ResourceNotFoundException;
import com.example.demo.infrastructure.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final VendedorService vendedorService;
    private final UsuarioService usuarioService;

    public ProdutoService(ProdutoRepository produtoRepository,
                          VendedorService vendedorService,
                          UsuarioService usuarioService) {
        this.produtoRepository = produtoRepository;
        this.vendedorService = vendedorService;
        this.usuarioService = usuarioService;
    }

    // ===================== Vitrine (público) =====================

    @Transactional(readOnly = true)
    public PageResponseDTO<ProdutoResponseDTO> listar(String nome, Pageable pageable) {
        Page<Produto> pagina = (nome == null || nome.isBlank())
                ? produtoRepository.findByProdutoStatus(ProdutoStatus.ATIVO, pageable)
                : produtoRepository.findByNomeContainingIgnoreCaseAndProdutoStatus(nome, ProdutoStatus.ATIVO, pageable);
        return PageResponseDTO.from(pagina.map(ProdutoResponseDTO::from));
    }

    @Transactional(readOnly = true)
    public ProdutoResponseDTO buscarPorId(Long id) {
        return ProdutoResponseDTO.from(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ProdutoResponseDTO> listarPorVendedor(Long vendedorId, Pageable pageable) {
        vendedorService.buscarEntidadePorId(vendedorId); // 404 se o vendedor não existir
        return PageResponseDTO.from(produtoRepository
                .findByVendedorIdAndProdutoStatus(vendedorId, ProdutoStatus.ATIVO, pageable)
                .map(ProdutoResponseDTO::from));
    }

    // ===================== Gestão (vendedor) =====================

    @Transactional(readOnly = true)
    public PageResponseDTO<ProdutoResponseDTO> listarMeusProdutos(String email, Pageable pageable) {
        Vendedor vendedor = vendedorService.buscarEntidadePorEmail(email);
        return PageResponseDTO.from(produtoRepository.findByVendedorId(vendedor.getId(), pageable)
                .map(ProdutoResponseDTO::from));
    }

    @Transactional
    public ProdutoResponseDTO criar(String email, ProdutoRequestDTO dto) {
        Vendedor vendedor = vendedorService.buscarEntidadePorEmail(email);

        if (produtoRepository.existsByEan(dto.ean())) {
            throw new ConflictException("EAN ja cadastrado: " + dto.ean());
        }

        Produto produto = new Produto();
        preencher(produto, dto);
        produto.setVendedor(vendedor);
        produto.setProdutoStatus(ProdutoStatus.ATIVO);
        ajustarStatusPorEstoque(produto);

        return ProdutoResponseDTO.from(produtoRepository.save(produto));
    }

    @Transactional
    public ProdutoResponseDTO atualizar(String email, Long id, ProdutoRequestDTO dto) {
        Produto produto = buscarEntidadePorId(id);
        validarDono(produto, email);

        if (produtoRepository.existsByEanAndIdNot(dto.ean(), id)) {
            throw new ConflictException("EAN ja cadastrado: " + dto.ean());
        }

        preencher(produto, dto);
        ajustarStatusPorEstoque(produto);

        return ProdutoResponseDTO.from(produtoRepository.save(produto));
    }

    @Transactional
    public ProdutoResponseDTO alterarStatus(String email, Long id, ProdutoStatus status) {
        if (status != ProdutoStatus.ATIVO && status != ProdutoStatus.INATIVO) {
            throw new RegraNegocioException("Só é possível definir manualmente os status ATIVO ou INATIVO");
        }

        Produto produto = buscarEntidadePorId(id);
        validarDono(produto, email);

        if (status == ProdutoStatus.ATIVO && produto.getQuantidade() == 0) {
            throw new RegraNegocioException("Não é possível ativar um produto sem estoque");
        }

        produto.setProdutoStatus(status);
        return ProdutoResponseDTO.from(produtoRepository.save(produto));
    }

    // Não apagamos produtos do banco, pois eles podem estar em pedidos: apenas inativamos
    @Transactional
    public void desativar(String email, Long id) {
        alterarStatus(email, id, ProdutoStatus.INATIVO);
    }

    // ===================== Auxiliares =====================

    public Produto buscarEntidadePorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + id));
    }

    // Estoque zerado => VENDIDO (esgotado). Reposição de um produto esgotado => volta a ATIVO
    public static void ajustarStatusPorEstoque(Produto produto) {
        if (produto.getQuantidade() == 0 && produto.getProdutoStatus() == ProdutoStatus.ATIVO) {
            produto.setProdutoStatus(ProdutoStatus.VENDIDO);
        } else if (produto.getQuantidade() > 0 && produto.getProdutoStatus() == ProdutoStatus.VENDIDO) {
            produto.setProdutoStatus(ProdutoStatus.ATIVO);
        }
    }

    private void validarDono(Produto produto, String email) {
        boolean dono = produto.getVendedor().getUsuario().getEmail().equals(email);
        if (!dono && !usuarioService.isAdmin(email)) {
            throw new AccessDeniedException("Você não é o vendedor deste produto");
        }
    }

    private void preencher(Produto produto, ProdutoRequestDTO dto) {
        produto.setNome(dto.nome());
        produto.setDescricao(dto.descricao());
        produto.setEan(dto.ean());
        produto.setPreco(dto.preco());
        produto.setQuantidade(dto.quantidade());
    }
}
