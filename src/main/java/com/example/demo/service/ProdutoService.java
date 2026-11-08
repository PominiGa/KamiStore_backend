package com.example.demo.service;

import com.example.demo.controller.dtos.request.ProdutoRequestDTO;
import com.example.demo.controller.dtos.request.VariacaoRequestDTO;
import com.example.demo.controller.dtos.response.PageResponseDTO;
import com.example.demo.controller.dtos.response.ProdutoResponseDTO;
import com.example.demo.controller.dtos.response.VariacaoResponseDTO;
import com.example.demo.infrastructure.entity.enums.ProdutoStatus;
import com.example.demo.infrastructure.entity.produtos.Produto;
import com.example.demo.infrastructure.entity.produtos.ProdutoImagem;
import com.example.demo.infrastructure.entity.produtos.ProdutoVariacao;
import com.example.demo.infrastructure.entity.usuarios.Vendedor;
import com.example.demo.infrastructure.exceptions.ConflictException;
import com.example.demo.infrastructure.exceptions.RegraNegocioException;
import com.example.demo.infrastructure.exceptions.ResourceNotFoundException;
import com.example.demo.infrastructure.repository.ImagemRef;
import com.example.demo.infrastructure.repository.ProdutoImagemRepository;
import com.example.demo.infrastructure.repository.ProdutoRepository;
import com.example.demo.infrastructure.repository.ProdutoVariacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProdutoService {

    // Limites de imagens por produto (o tamanho máximo por arquivo fica no application.properties)
    private static final int MAX_IMAGENS_POR_PRODUTO = 10;
    private static final Set<String> TIPOS_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final ProdutoRepository produtoRepository;
    private final ProdutoImagemRepository produtoImagemRepository;
    private final ProdutoVariacaoRepository produtoVariacaoRepository;
    private final VendedorService vendedorService;
    private final UsuarioService usuarioService;

    public ProdutoService(ProdutoRepository produtoRepository,
                          ProdutoImagemRepository produtoImagemRepository,
                          ProdutoVariacaoRepository produtoVariacaoRepository,
                          VendedorService vendedorService,
                          UsuarioService usuarioService) {
        this.produtoRepository = produtoRepository;
        this.produtoImagemRepository = produtoImagemRepository;
        this.produtoVariacaoRepository = produtoVariacaoRepository;
        this.vendedorService = vendedorService;
        this.usuarioService = usuarioService;
    }

    // ===================== Vitrine (público) =====================

    @Transactional(readOnly = true)
    public PageResponseDTO<ProdutoResponseDTO> listar(String nome, Pageable pageable) {
        Page<Produto> pagina = (nome == null || nome.isBlank())
                ? produtoRepository.findByProdutoStatus(ProdutoStatus.ATIVO, pageable)
                : produtoRepository.findByNomeContainingIgnoreCaseAndProdutoStatus(nome, ProdutoStatus.ATIVO, pageable);
        return toPage(pagina);
    }

    @Transactional(readOnly = true)
    public ProdutoResponseDTO buscarPorId(Long id) {
        return toDTO(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ProdutoResponseDTO> listarPorVendedor(Long vendedorId, Pageable pageable) {
        vendedorService.buscarEntidadePorId(vendedorId); // 404 se o vendedor não existir
        return toPage(produtoRepository.findByVendedorIdAndProdutoStatus(vendedorId, ProdutoStatus.ATIVO, pageable));
    }

    // ===================== Gestão (vendedor) =====================

    @Transactional(readOnly = true)
    public PageResponseDTO<ProdutoResponseDTO> listarMeusProdutos(String email, Pageable pageable) {
        Vendedor vendedor = vendedorService.buscarEntidadePorEmail(email);
        return toPage(produtoRepository.findByVendedorId(vendedor.getId(), pageable));
    }

    @Transactional
    public ProdutoResponseDTO criar(String email, ProdutoRequestDTO dto) {
        Vendedor vendedor = vendedorService.buscarEntidadePorEmail(email);

        if (produtoRepository.existsByEan(dto.ean())) {
            throw new ConflictException("EAN ja cadastrado: " + dto.ean());
        }

        validarVariacoes(dto.variacoes());

        Produto produto = new Produto();
        preencher(produto, dto);
        produto.setVendedor(vendedor);
        produto.setProdutoStatus(ProdutoStatus.ATIVO);
        produto.setQuantidade(0); // definido de verdade em sincronizarVariacoes
        produto = produtoRepository.save(produto);

        sincronizarVariacoes(produto, dto);
        ajustarStatusPorEstoque(produto);

        return toDTO(produtoRepository.save(produto));
    }

    @Transactional
    public ProdutoResponseDTO atualizar(String email, Long id, ProdutoRequestDTO dto) {
        Produto produto = buscarEntidadePorId(id);
        validarDono(produto, email);

        if (produtoRepository.existsByEanAndIdNot(dto.ean(), id)) {
            throw new ConflictException("EAN ja cadastrado: " + dto.ean());
        }

        validarVariacoes(dto.variacoes());

        preencher(produto, dto);
        sincronizarVariacoes(produto, dto);
        ajustarStatusPorEstoque(produto);

        return toDTO(produtoRepository.save(produto));
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
        return toDTO(produtoRepository.save(produto));
    }

    // Não apagamos produtos do banco, pois eles podem estar em pedidos: apenas inativamos
    @Transactional
    public void desativar(String email, Long id) {
        alterarStatus(email, id, ProdutoStatus.INATIVO);
    }

    // ===================== Imagens =====================

    @Transactional
    public ProdutoResponseDTO adicionarImagens(String email, Long produtoId, List<MultipartFile> arquivos) {
        Produto produto = buscarEntidadePorId(produtoId);
        validarDono(produto, email);

        List<MultipartFile> validos = arquivos == null ? List.of()
                : arquivos.stream().filter(a -> a != null && !a.isEmpty()).toList();
        if (validos.isEmpty()) {
            throw new RegraNegocioException("Envie ao menos uma imagem no campo 'imagens'");
        }

        long total = produtoImagemRepository.countByProdutoId(produtoId) + validos.size();
        if (total > MAX_IMAGENS_POR_PRODUTO) {
            throw new RegraNegocioException("Um produto pode ter no máximo " + MAX_IMAGENS_POR_PRODUTO + " imagens");
        }

        for (MultipartFile arquivo : validos) {
            String contentType = arquivo.getContentType();
            if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType.toLowerCase())) {
                throw new RegraNegocioException("Formato não suportado: " + arquivo.getOriginalFilename()
                        + ". Use JPEG, PNG, WEBP ou GIF");
            }

            ProdutoImagem imagem = new ProdutoImagem();
            imagem.setProduto(produto);
            imagem.setNomeArquivo(Objects.requireNonNullElse(arquivo.getOriginalFilename(), "imagem"));
            imagem.setContentType(contentType.toLowerCase());
            imagem.setTamanho(arquivo.getSize());
            try {
                imagem.setDados(arquivo.getBytes());
            } catch (IOException e) {
                throw new RegraNegocioException("Não foi possível ler o arquivo " + arquivo.getOriginalFilename());
            }
            produtoImagemRepository.save(imagem);
        }

        return toDTO(produto);
    }

    @Transactional(readOnly = true)
    public ProdutoImagem buscarImagem(Long produtoId, Long imagemId) {
        return produtoImagemRepository.findByIdAndProdutoId(imagemId, produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Imagem não encontrada: " + imagemId));
    }

    @Transactional
    public void removerImagem(String email, Long produtoId, Long imagemId) {
        Produto produto = buscarEntidadePorId(produtoId);
        validarDono(produto, email);
        if (produtoImagemRepository.deleteByIdAndProdutoId(imagemId, produtoId) == 0) {
            throw new ResourceNotFoundException("Imagem não encontrada: " + imagemId);
        }
    }

    // ===================== Auxiliares =====================

    private ProdutoResponseDTO toDTO(Produto produto) {
        List<Long> imagemIds = produtoImagemRepository.findRefsByProdutoIds(List.of(produto.getId())).stream()
                .map(ImagemRef::imagemId)
                .toList();
        List<VariacaoResponseDTO> variacoes = produtoVariacaoRepository
                .findByProdutoIdAndAtivoTrueOrderById(produto.getId()).stream()
                .map(VariacaoResponseDTO::from)
                .toList();
        return ProdutoResponseDTO.from(produto, imagemIds, variacoes);
    }

    // Busca imagens e variações de todos os produtos da página de uma vez (evita uma consulta por produto)
    private PageResponseDTO<ProdutoResponseDTO> toPage(Page<Produto> pagina) {
        List<Long> produtoIds = pagina.getContent().stream().map(Produto::getId).toList();
        if (produtoIds.isEmpty()) {
            return PageResponseDTO.from(pagina.map(p -> ProdutoResponseDTO.from(p, List.of(), List.of())));
        }
        Map<Long, List<Long>> imagensPorProduto = produtoImagemRepository.findRefsByProdutoIds(produtoIds).stream()
                .collect(Collectors.groupingBy(ImagemRef::produtoId,
                        Collectors.mapping(ImagemRef::imagemId, Collectors.toList())));
        Map<Long, List<VariacaoResponseDTO>> variacoesPorProduto = produtoVariacaoRepository
                .findByProdutoIdInAndAtivoTrueOrderById(produtoIds).stream()
                .collect(Collectors.groupingBy(v -> v.getProduto().getId(),
                        Collectors.mapping(VariacaoResponseDTO::from, Collectors.toList())));
        return PageResponseDTO.from(pagina.map(p -> ProdutoResponseDTO.from(p,
                imagensPorProduto.getOrDefault(p.getId(), List.of()),
                variacoesPorProduto.getOrDefault(p.getId(), List.of()))));
    }

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
    }

    // ===================== Variações (cor / tamanho) =====================

    // Cria/atualiza/remove as variações conforme a lista enviada e recalcula o estoque do produto.
    // Com variações ativas, o estoque do produto é a soma do estoque delas
    private void sincronizarVariacoes(Produto produto, ProdutoRequestDTO dto) {
        if (dto.variacoes() != null) {
            Map<Long, ProdutoVariacao> existentes = new HashMap<>();
            produtoVariacaoRepository.findByProdutoIdAndAtivoTrueOrderById(produto.getId())
                    .forEach(v -> existentes.put(v.getId(), v));

            for (VariacaoRequestDTO req : dto.variacoes()) {
                ProdutoVariacao variacao;
                if (req.id() != null) {
                    variacao = existentes.remove(req.id());
                    if (variacao == null) {
                        throw new ResourceNotFoundException("Variação não encontrada neste produto: " + req.id());
                    }
                } else {
                    variacao = new ProdutoVariacao();
                    variacao.setProduto(produto);
                }
                variacao.setCor(normalizar(req.cor()));
                variacao.setTamanho(normalizar(req.tamanho()));
                variacao.setPreco(req.preco());
                variacao.setQuantidade(req.quantidade());
                produtoVariacaoRepository.save(variacao);
            }

            // As que não vieram na lista são removidas (logicamente, pois podem estar em pedidos)
            existentes.values().forEach(v -> {
                v.setAtivo(false);
                v.setQuantidade(0);
            });
        }

        List<ProdutoVariacao> ativas = produtoVariacaoRepository.findByProdutoIdAndAtivoTrueOrderById(produto.getId());
        if (!ativas.isEmpty()) {
            produto.setQuantidade(ativas.stream().mapToInt(ProdutoVariacao::getQuantidade).sum());
        } else if (dto.quantidade() != null) {
            produto.setQuantidade(dto.quantidade());
        } else {
            throw new RegraNegocioException("Informe a quantidade em estoque ou cadastre variações");
        }
    }

    private void validarVariacoes(List<VariacaoRequestDTO> variacoes) {
        if (variacoes == null) return;
        Set<String> combinacoes = new HashSet<>();
        Set<Long> ids = new HashSet<>();
        for (VariacaoRequestDTO v : variacoes) {
            String cor = normalizar(v.cor());
            String tamanho = normalizar(v.tamanho());
            if (cor == null && tamanho == null) {
                throw new RegraNegocioException("Cada variação precisa ter cor e/ou tamanho");
            }
            String chave = (cor == null ? "" : cor.toLowerCase()) + "|" + (tamanho == null ? "" : tamanho.toLowerCase());
            if (!combinacoes.add(chave)) {
                throw new RegraNegocioException("Variação repetida: " + (cor == null ? "" : cor)
                        + (cor != null && tamanho != null ? " / " : "") + (tamanho == null ? "" : tamanho));
            }
            if (v.id() != null && !ids.add(v.id())) {
                throw new RegraNegocioException("Variação repetida: id " + v.id());
            }
        }
    }

    private static String normalizar(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    // Usado pelo carrinho: retorna a variação escolhida (obrigatória se o produto tiver variações)
    public ProdutoVariacao resolverVariacao(Produto produto, Long variacaoId) {
        if (variacaoId == null) {
            if (produtoVariacaoRepository.existsByProdutoIdAndAtivoTrue(produto.getId())) {
                throw new RegraNegocioException("Escolha uma variação (cor/tamanho) para " + produto.getNome());
            }
            return null;
        }
        return produtoVariacaoRepository.findByIdAndProdutoIdAndAtivoTrue(variacaoId, produto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Variação não encontrada neste produto: " + variacaoId));
    }

    public boolean possuiVariacoes(Long produtoId) {
        return produtoVariacaoRepository.existsByProdutoIdAndAtivoTrue(produtoId);
    }
}
