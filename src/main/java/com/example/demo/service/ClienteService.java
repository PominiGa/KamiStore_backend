package com.example.demo.service;

import com.example.demo.controller.dtos.request.ClienteRequestDTO;
import com.example.demo.controller.dtos.response.ClienteResponseDTO;
import com.example.demo.infrastructure.entity.enums.UsuarioRole;
import com.example.demo.infrastructure.entity.produtos.Carrinho;
import com.example.demo.infrastructure.entity.usuarios.Cliente;
import com.example.demo.infrastructure.entity.usuarios.Usuario;
import com.example.demo.infrastructure.exceptions.ConflictException;
import com.example.demo.infrastructure.exceptions.ResourceNotFoundException;
import com.example.demo.infrastructure.repository.ClienteRepository;
import com.example.demo.infrastructure.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    public ClienteService(ClienteRepository clienteRepository,
                          UsuarioRepository usuarioRepository,
                          UsuarioService usuarioService) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public ClienteResponseDTO criarCliente(ClienteRequestDTO dto) {
        String cpf = dto.cpf().replaceAll("\\D", "");

        validarCpf(cpf);

        Usuario usuario = usuarioService.novoUsuario(dto.nome(), dto.email(), dto.senha(),
                dto.enderecos(), dto.telefones(), UsuarioRole.CLIENTE);

        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setCpf(cpf);

        // Todo cliente nasce com um carrinho vazio
        Carrinho carrinho = new Carrinho();
        carrinho.setCliente(cliente);
        carrinho.setItens(new ArrayList<>());
        cliente.setCarrinho(carrinho);

        usuario.setCliente(cliente);
        usuarioRepository.save(usuario); // cascade salva cliente e carrinho

        return ClienteResponseDTO.from(cliente);
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarMeuPerfil(String email) {
        return ClienteResponseDTO.from(buscarEntidadePorEmail(email));
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .map(ClienteResponseDTO::from)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarTodos() {
        return clienteRepository.findAll().stream().map(ClienteResponseDTO::from).toList();
    }

    public Cliente buscarEntidadePorEmail(String email) {
        return clienteRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado para o usuário: " + email));
    }

    private boolean validadorCpf(String cpf) {
        if (cpf == null) {
            return false;
        }

        cpf = cpf.replaceAll("\\D", "");

        if (cpf.length() != 11 || cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        int soma = 0;
        int peso = 10;

        for (int i = 0; i < 9; i++) {
            soma += (cpf.charAt(i) - '0') * peso;
            peso--;
        }

        int digito10 = 11 - (soma % 11);

        if (digito10 >= 10) {
            digito10 = 0;
        }

        soma = 0;
        peso = 11;

        for (int i = 0; i < 10; i++) {
            soma += (cpf.charAt(i) - '0') * peso;
            peso--;
        }

        int digito11 = 11 - (soma % 11);

        if (digito11 >= 10) {
            digito11 = 0;
        }

        return digito10 == cpf.charAt(9) - '0'
                && digito11 == cpf.charAt(10) - '0';
    }

    private void validarCpf(String cpf) {
        if (!validadorCpf(cpf)) {
            throw new ConflictException("CPF invalido: " + cpf);
        }

        if (clienteRepository.existsByCpf(cpf)) {
            throw new ConflictException("CPF ja cadastrado: " + cpf);
        }
    }
}
