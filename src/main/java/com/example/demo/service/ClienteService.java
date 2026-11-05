package com.example.demo.service;

import com.example.demo.infrastructure.entity.enums.UsuarioRole;
import com.example.demo.infrastructure.entity.usuarios.Cliente;
import com.example.demo.infrastructure.entity.usuarios.Usuario;
import com.example.demo.infrastructure.exceptions.ConflictException;
import com.example.demo.infrastructure.repository.ClienteRepository;
import com.example.demo.infrastructure.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    public ClienteService(ClienteRepository clienteRepository,
                          UsuarioRepository usuarioRepository) {
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Cliente criarCliente(Usuario usuario, String cpf) {
        cpf = cpf.replaceAll("\\D", "");

        validarCpf(cpf);

        usuario.setUsuarioRole(UsuarioRole.CLIENTE);
        Cliente cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setCpf(cpf);

        return clienteRepository.save(cliente);
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

