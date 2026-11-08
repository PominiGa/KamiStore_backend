package com.example.demo.service;

import com.example.demo.controller.dtos.request.PedidoRequestDTO;
import com.example.demo.controller.dtos.response.PedidoResponseDTO;
import com.example.demo.infrastructure.entity.enums.PedidoStatus;
import com.example.demo.infrastructure.entity.enums.ProdutoStatus;
import com.example.demo.infrastructure.entity.enums.UsuarioRole;
import com.example.demo.infrastructure.entity.produtos.Carrinho;
import com.example.demo.infrastructure.entity.produtos.ItemCarrinho;
import com.example.demo.infrastructure.entity.produtos.ItemPedido;
import com.example.demo.infrastructure.entity.produtos.Pedido;
import com.example.demo.infrastructure.entity.produtos.Produto;
import com.example.demo.infrastructure.entity.produtos.ProdutoVariacao;
import com.example.demo.infrastructure.entity.usuarios.Cliente;
import com.example.demo.infrastructure.entity.usuarios.Endereco;
import com.example.demo.infrastructure.entity.usuarios.Usuario;
import com.example.demo.infrastructure.entity.usuarios.Vendedor;
import com.example.demo.infrastructure.exceptions.RegraNegocioException;
import com.example.demo.infrastructure.exceptions.ResourceNotFoundException;
import com.example.demo.infrastructure.repository.PedidoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final EntityManager entityManager;
    private final ClienteService clienteService;
    private final VendedorService vendedorService;
    private final UsuarioService usuarioService;
    private final CarrinhoService carrinhoService;
    private final ProdutoService produtoService;

    public PedidoService(PedidoRepository pedidoRepository,
                         EntityManager entityManager,
                         ClienteService clienteService,
                         VendedorService vendedorService,
                         UsuarioService usuarioService,
                         CarrinhoService carrinhoService,
                         ProdutoService produtoService) {
        this.pedidoRepository = pedidoRepository;
        this.entityManager = entityManager;
        this.clienteService = clienteService;
        this.vendedorService = vendedorService;
        this.usuarioService = usuarioService;
        this.carrinhoService = carrinhoService;
        this.produtoService = produtoService;
    }

    // Checkout: transforma o carrinho do cliente em um pedido e baixa o estoque
    @Transactional
    public PedidoResponseDTO criarPedido(String email, PedidoRequestDTO dto) {
        Cliente cliente = clienteService.buscarEntidadePorEmail(email);
        Carrinho carrinho = carrinhoService.carrinhoDoCliente(email);

        if (carrinho.getItens().isEmpty()) {
            throw new RegraNegocioException("O carrinho está vazio");
        }

        Endereco endereco = cliente.getUsuario().getEnderecos().stream()
                .filter(e -> e.getId().equals(dto.enderecoId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado: " + dto.enderecoId()));

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setStatus(PedidoStatus.AGUARDANDO_PAGAMENTO);
        pedido.setDataPedido(LocalDateTime.now());
        pedido.setEnderecoEntrega(formatarEndereco(endereco));
        pedido.setItens(new ArrayList<>());

        BigDecimal total = BigDecimal.ZERO;

        // Ordena por produto/variação para travar sempre na mesma ordem (evita deadlock entre compras simultâneas)
        List<ItemCarrinho> itensCarrinho = carrinho.getItens().stream()
                .sorted(Comparator.comparing((ItemCarrinho i) -> i.getProduto().getId())
                        .thenComparing(i -> i.getVariacao() != null ? i.getVariacao().getId() : 0L))
                .toList();

        Set<Long> produtosTravados = new HashSet<>();
        for (ItemCarrinho itemCarrinho : itensCarrinho) {
            Produto produto = travarProduto(itemCarrinho.getProduto(), produtosTravados);
            ProdutoVariacao variacao = itemCarrinho.getVariacao();
            int quantidade = itemCarrinho.getQuantidade();

            if (produto.getProdutoStatus() != ProdutoStatus.ATIVO) {
                throw new RegraNegocioException("Produto indisponível: " + produto.getNome());
            }

            BigDecimal preco;
            if (variacao != null) {
                entityManager.refresh(variacao, LockModeType.PESSIMISTIC_WRITE);
                String nome = produto.getNome() + " (" + variacao.getDescricao() + ")";
                if (!variacao.isAtivo()) {
                    throw new RegraNegocioException("Variação indisponível: " + nome);
                }
                if (variacao.getQuantidade() < quantidade) {
                    throw new RegraNegocioException("Estoque insuficiente para " + nome
                            + ". Disponível: " + variacao.getQuantidade());
                }
                variacao.setQuantidade(variacao.getQuantidade() - quantidade);
                preco = variacao.getPrecoEfetivo();
            } else {
                if (produtoService.possuiVariacoes(produto.getId())) {
                    throw new RegraNegocioException("Escolha uma variação (cor/tamanho) para " + produto.getNome());
                }
                if (produto.getQuantidade() < quantidade) {
                    throw new RegraNegocioException("Estoque insuficiente para " + produto.getNome()
                            + ". Disponível: " + produto.getQuantidade());
                }
                preco = produto.getPreco();
            }

            // O estoque do produto é a soma das variações, então baixa nos dois
            produto.setQuantidade(produto.getQuantidade() - quantidade);
            ProdutoService.ajustarStatusPorEstoque(produto);

            ItemPedido itemPedido = new ItemPedido();
            itemPedido.setPedido(pedido);
            itemPedido.setProduto(produto);
            itemPedido.setVariacao(variacao);
            itemPedido.setQuantidade(quantidade);
            itemPedido.setPrecoUnitario(preco); // guarda o preço do momento da compra
            pedido.getItens().add(itemPedido);

            total = total.add(preco.multiply(BigDecimal.valueOf(quantidade)));
        }

        pedido.setValorTotal(total);
        Pedido salvo = pedidoRepository.save(pedido);

        carrinho.getItens().clear();

        return PedidoResponseDTO.from(salvo);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarMeusPedidos(String email) {
        Cliente cliente = clienteService.buscarEntidadePorEmail(email);
        return pedidoRepository.findByClienteIdOrderByDataPedidoDesc(cliente.getId())
                .stream().map(PedidoResponseDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarVendas(String email) {
        Vendedor vendedor = vendedorService.buscarEntidadePorEmail(email);
        return pedidoRepository.findPedidosComProdutosDoVendedor(vendedor.getId())
                .stream().map(PedidoResponseDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listarTodos() {
        return pedidoRepository.findAll().stream().map(PedidoResponseDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponseDTO buscarPorId(String email, Long id) {
        Pedido pedido = buscarEntidadePorId(id);
        validarAcesso(pedido, usuarioService.buscarEntidadePorEmail(email));
        return PedidoResponseDTO.from(pedido);
    }

    // Cliente cancela o próprio pedido (somente antes da preparação)
    @Transactional
    public PedidoResponseDTO cancelar(String email, Long id) {
        Pedido pedido = buscarEntidadePorId(id);

        if (!pedido.getCliente().getUsuario().getEmail().equals(email)) {
            throw new AccessDeniedException("Este pedido não pertence a você");
        }

        mudarStatus(pedido, PedidoStatus.CANCELADO);
        return PedidoResponseDTO.from(pedido);
    }

    // Admin ou vendedor de algum item do pedido avança o status (pagamento, envio, entrega...)
    @Transactional
    public PedidoResponseDTO atualizarStatus(String email, Long id, PedidoStatus novoStatus) {
        Pedido pedido = buscarEntidadePorId(id);
        Usuario usuario = usuarioService.buscarEntidadePorEmail(email);

        boolean admin = usuario.getUsuarioRole() == UsuarioRole.ADMIN;
        if (!admin && !vendedorParticipa(pedido, email)) {
            throw new AccessDeniedException("Você não tem permissão para alterar este pedido");
        }

        mudarStatus(pedido, novoStatus);
        return PedidoResponseDTO.from(pedido);
    }

    // ===================== Auxiliares =====================

    private void mudarStatus(Pedido pedido, PedidoStatus novoStatus) {
        if (!pedido.getStatus().podeMudarPara(novoStatus)) {
            throw new RegraNegocioException("Não é possível alterar o pedido de "
                    + pedido.getStatus() + " para " + novoStatus);
        }
        if (novoStatus == PedidoStatus.CANCELADO) {
            devolverEstoque(pedido);
        }
        pedido.setStatus(novoStatus);
    }

    private void devolverEstoque(Pedido pedido) {
        Set<Long> produtosTravados = new HashSet<>();
        List<ItemPedido> itens = pedido.getItens().stream()
                .sorted(Comparator.comparing((ItemPedido i) -> i.getProduto().getId())
                        .thenComparing(i -> i.getVariacao() != null ? i.getVariacao().getId() : 0L))
                .toList();
        for (ItemPedido item : itens) {
            Produto produto = travarProduto(item.getProduto(), produtosTravados);
            ProdutoVariacao variacao = item.getVariacao();
            if (variacao != null) {
                entityManager.refresh(variacao, LockModeType.PESSIMISTIC_WRITE);
                if (!variacao.isAtivo()) {
                    continue; // variação foi removida pelo vendedor: não há onde devolver o estoque
                }
                variacao.setQuantidade(variacao.getQuantidade() + item.getQuantidade());
            }
            produto.setQuantidade(produto.getQuantidade() + item.getQuantidade());
            ProdutoService.ajustarStatusPorEstoque(produto);
        }
    }

    // Trava a linha do produto e relê o estoque do banco (evita vender mais do que existe em compras simultâneas).
    // Só faz o refresh na 1ª vez: o mesmo produto pode aparecer em vários itens (variações diferentes) e um
    // segundo refresh descartaria a baixa de estoque já feita em memória
    private Produto travarProduto(Produto produto, Set<Long> jaTravados) {
        if (jaTravados.add(produto.getId())) {
            entityManager.refresh(produto, LockModeType.PESSIMISTIC_WRITE);
        }
        return produto;
    }

    private void validarAcesso(Pedido pedido, Usuario usuario) {
        boolean permitido = switch (usuario.getUsuarioRole()) {
            case ADMIN -> true;
            case CLIENTE -> pedido.getCliente().getUsuario().getEmail().equals(usuario.getEmail());
            case VENDEDOR -> vendedorParticipa(pedido, usuario.getEmail());
        };
        if (!permitido) {
            throw new AccessDeniedException("Você não tem acesso a este pedido");
        }
    }

    private boolean vendedorParticipa(Pedido pedido, String email) {
        return pedido.getItens().stream()
                .anyMatch(i -> i.getProduto().getVendedor().getUsuario().getEmail().equals(email));
    }

    private Pedido buscarEntidadePorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado: " + id));
    }

    private String formatarEndereco(Endereco e) {
        String complemento = (e.getComplemento() == null || e.getComplemento().isBlank()) ? "" : " - " + e.getComplemento();
        return "%s, %d%s - %s/%s - CEP %s".formatted(e.getRua(), e.getNumero(), complemento,
                e.getCidade(), e.getEstado(), e.getCep());
    }
}
