package com.example.demo.service;

import com.example.demo.controller.dtos.request.ItemCarrinhoRequestDTO;
import com.example.demo.controller.dtos.response.CarrinhoResponseDTO;
import com.example.demo.infrastructure.entity.enums.ProdutoStatus;
import com.example.demo.infrastructure.entity.produtos.Carrinho;
import com.example.demo.infrastructure.entity.produtos.ItemCarrinho;
import com.example.demo.infrastructure.entity.produtos.Produto;
import com.example.demo.infrastructure.entity.usuarios.Cliente;
import com.example.demo.infrastructure.exceptions.RegraNegocioException;
import com.example.demo.infrastructure.exceptions.ResourceNotFoundException;
import com.example.demo.infrastructure.repository.CarrinhoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

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

        Optional<ItemCarrinho> existente = buscarItem(carrinho, produto.getId());
        int novaQuantidade = existente.map(ItemCarrinho::getQuantidade).orElse(0) + dto.quantidade();
        validarEstoque(produto, novaQuantidade);

        if (existente.isPresent()) {
            existente.get().setQuantidade(novaQuantidade);
        } else {
            ItemCarrinho item = new ItemCarrinho();
            item.setCarrinho(carrinho);
            item.setProduto(produto);
            item.setQuantidade(novaQuantidade);
            carrinho.getItens().add(item);
        }

        return CarrinhoResponseDTO.from(carrinhoRepository.save(carrinho));
    }

    @Transactional
    public CarrinhoResponseDTO atualizarQuantidade(String email, Long produtoId, Integer quantidade) {
        Carrinho carrinho = carrinhoDoCliente(email);
        ItemCarrinho item = buscarItem(carrinho, produtoId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não está no carrinho: " + produtoId));

        validarEstoque(item.getProduto(), quantidade);
        item.setQuantidade(quantidade);

        return CarrinhoResponseDTO.from(carrinhoRepository.save(carrinho));
    }

    @Transactional
    public CarrinhoResponseDTO removerItem(String email, Long produtoId) {
        Carrinho carrinho = carrinhoDoCliente(email);
        boolean removido = carrinho.getItens().removeIf(i -> i.getProduto().getId().equals(produtoId));
        if (!removido) {
            throw new ResourceNotFoundException("Produto não está no carrinho: " + produtoId);
        }
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

    private Optional<ItemCarrinho> buscarItem(Carrinho carrinho, Long produtoId) {
        return carrinho.getItens().stream()
                .filter(i -> i.getProduto().getId().equals(produtoId))
                .findFirst();
    }

    private void validarEstoque(Produto produto, int quantidade) {
        if (quantidade > produto.getQuantidade()) {
            throw new RegraNegocioException("Estoque insuficiente para " + produto.getNome()
                    + ". Disponível: " + produto.getQuantidade());
        }
    }
}
