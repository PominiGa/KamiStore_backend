package com.example.demo.service;

import com.example.demo.controller.dtos.request.ItemCarrinhoRequestDTO;
import com.example.demo.controller.dtos.response.CarrinhoResponseDTO;
import com.example.demo.infrastructure.entity.enums.ProdutoStatus;
import com.example.demo.infrastructure.entity.produtos.Carrinho;
import com.example.demo.infrastructure.entity.produtos.ItemCarrinho;
import com.example.demo.infrastructure.entity.produtos.Produto;
import com.example.demo.infrastructure.entity.produtos.ProdutoVariacao;
import com.example.demo.infrastructure.entity.usuarios.Cliente;
import com.example.demo.infrastructure.exceptions.RegraNegocioException;
import com.example.demo.infrastructure.exceptions.ResourceNotFoundException;
import com.example.demo.infrastructure.repository.CarrinhoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CarrinhoService {

    private final CarrinhoRepository carrinhoRepository;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;

    public CarrinhoService(CarrinhoRepository carrinhoRepository,
                           ClienteService clienteService,
                           ProdutoService produtoService) {
        this.carrinhoRepository = carrinhoRepository;
        this.clienteService = clienteService;
        this.produtoService = produtoService;
    }

    @Transactional
    public CarrinhoResponseDTO buscar(String email) {
        return CarrinhoResponseDTO.from(carrinhoDoCliente(email));
    }

    // Adiciona o produto ao carrinho; se já existir, soma a quantidade
    @Transactional
    public CarrinhoResponseDTO adicionarItem(String email, ItemCarrinhoRequestDTO dto) {
        Carrinho carrinho = carrinhoDoCliente(email);
        Produto produto = produtoService.buscarEntidadePorId(dto.produtoId());

        if (produto.getProdutoStatus() != ProdutoStatus.ATIVO) {
            throw new RegraNegocioException("Produto indisponível para compra: " + produto.getNome());
        }

        ProdutoVariacao variacao = produtoService.resolverVariacao(produto, dto.variacaoId());

        Optional<ItemCarrinho> existente = carrinho.getItens().stream()
                .filter(i -> i.getProduto().getId().equals(produto.getId())
                        && Objects.equals(idDaVariacao(i), dto.variacaoId()))
                .findFirst();
        int novaQuantidade = existente.map(ItemCarrinho::getQuantidade).orElse(0) + dto.quantidade();
        validarEstoque(produto, variacao, novaQuantidade);

        if (existente.isPresent()) {
            existente.get().setQuantidade(novaQuantidade);
        } else {
            ItemCarrinho item = new ItemCarrinho();
            item.setCarrinho(carrinho);
            item.setProduto(produto);
            item.setVariacao(variacao);
            item.setQuantidade(novaQuantidade);
            carrinho.getItens().add(item);
        }

        return CarrinhoResponseDTO.from(carrinhoRepository.save(carrinho));
    }

    @Transactional
    public CarrinhoResponseDTO atualizarQuantidade(String email, Long produtoId, Long variacaoId, Integer quantidade) {
        Carrinho carrinho = carrinhoDoCliente(email);
        ItemCarrinho item = buscarItem(carrinho, produtoId, variacaoId);

        validarEstoque(item.getProduto(), item.getVariacao(), quantidade);
        item.setQuantidade(quantidade);

        return CarrinhoResponseDTO.from(carrinhoRepository.save(carrinho));
    }

    @Transactional
    public CarrinhoResponseDTO removerItem(String email, Long produtoId, Long variacaoId) {
        Carrinho carrinho = carrinhoDoCliente(email);
        carrinho.getItens().remove(buscarItem(carrinho, produtoId, variacaoId));
        return CarrinhoResponseDTO.from(carrinhoRepository.save(carrinho));
    }

    @Transactional
    public void limpar(String email) {
        Carrinho carrinho = carrinhoDoCliente(email);
        carrinho.getItens().clear();
        carrinhoRepository.save(carrinho);
    }

    // Retorna o carrinho do cliente logado (cria um se, por algum motivo, ainda não existir)
    public Carrinho carrinhoDoCliente(String email) {
        Cliente cliente = clienteService.buscarEntidadePorEmail(email);
        Carrinho carrinho = cliente.getCarrinho();
        if (carrinho == null) {
            carrinho = new Carrinho();
            carrinho.setCliente(cliente);
            carrinho.setItens(new ArrayList<>());
            carrinho = carrinhoRepository.save(carrinho);
            cliente.setCarrinho(carrinho);
        }
        if (carrinho.getItens() == null) {
            carrinho.setItens(new ArrayList<>());
        }
        return carrinho;
    }

    // Sem variacaoId: aceita apenas se houver um único item desse produto no carrinho
    private ItemCarrinho buscarItem(Carrinho carrinho, Long produtoId, Long variacaoId) {
        List<ItemCarrinho> doProduto = carrinho.getItens().stream()
                .filter(i -> i.getProduto().getId().equals(produtoId))
                .filter(i -> variacaoId == null || variacaoId.equals(idDaVariacao(i)))
                .toList();
        if (doProduto.isEmpty()) {
            throw new ResourceNotFoundException("Produto não está no carrinho: " + produtoId
                    + (variacaoId != null ? " (variação " + variacaoId + ")" : ""));
        }
        if (doProduto.size() > 1) {
            throw new RegraNegocioException("Há mais de uma variação deste produto no carrinho: informe o variacaoId");
        }
        return doProduto.get(0);
    }

    private static Long idDaVariacao(ItemCarrinho item) {
        return item.getVariacao() != null ? item.getVariacao().getId() : null;
    }

    private void validarEstoque(Produto produto, ProdutoVariacao variacao, int quantidade) {
        int disponivel = variacao != null ? variacao.getQuantidade() : produto.getQuantidade();
        if (quantidade > disponivel) {
            String nome = produto.getNome() + (variacao != null ? " (" + variacao.getDescricao() + ")" : "");
            throw new RegraNegocioException("Estoque insuficiente para " + nome + ". Disponível: " + disponivel);
        }
    }
}
