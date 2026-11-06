package com.example.demo.service;

import com.example.demo.controller.dtos.request.VendedorRequestDTO;
import com.example.demo.controller.dtos.response.VendedorResponseDTO;
import com.example.demo.infrastructure.entity.enums.UsuarioRole;
import com.example.demo.infrastructure.entity.usuarios.Usuario;
import com.example.demo.infrastructure.entity.usuarios.Vendedor;
import com.example.demo.infrastructure.exceptions.ConflictException;
import com.example.demo.infrastructure.exceptions.ResourceNotFoundException;
import com.example.demo.infrastructure.repository.UsuarioRepository;
import com.example.demo.infrastructure.repository.VendedorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VendedorService {

    private final VendedorRepository vendedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    public VendedorService(VendedorRepository vendedorRepository,
                           UsuarioRepository usuarioRepository,
                           UsuarioService usuarioService) {
        this.vendedorRepository = vendedorRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public VendedorResponseDTO criarVendedor(VendedorRequestDTO dto) {
        String cnpj = dto.cnpj().replaceAll("\\D", "");

        validarCnpj(cnpj);

        Usuario usuario = usuarioService.novoUsuario(dto.nome(), dto.email(), dto.senha(),
                dto.enderecos(), dto.telefones(), UsuarioRole.VENDEDOR);

        Vendedor vendedor = new Vendedor();
        vendedor.setUsuario(usuario);
        vendedor.setCnpj(cnpj);

        usuario.setVendedor(vendedor);
        usuarioRepository.save(usuario); // cascade salva o vendedor

        return VendedorResponseDTO.from(vendedor);
    }

    @Transactional(readOnly = true)
    public VendedorResponseDTO buscarMeuPerfil(String email) {
        return VendedorResponseDTO.from(buscarEntidadePorEmail(email));
    }

    @Transactional(readOnly = true)
    public VendedorResponseDTO buscarPorId(Long id) {
        return VendedorResponseDTO.from(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public List<VendedorResponseDTO> listarTodos() {
        return vendedorRepository.findAll().stream().map(VendedorResponseDTO::from).toList();
    }

    public Vendedor buscarEntidadePorEmail(String email) {
        return vendedorRepository.findByUsuarioEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor não encontrado para o usuário: " + email));
    }

    public Vendedor buscarEntidadePorId(Long id) {
        return vendedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor não encontrado: " + id));
    }

    public void validarCnpj(String cnpj) {
        if (!isCNPJ(cnpj)) {
            throw new ConflictException("CNPJ invalido: " + cnpj);
        }

        if (vendedorRepository.existsByCnpj(cnpj)) {
            throw new ConflictException("CNPJ ja cadastrado: " + cnpj);
        }
    }

    private static boolean isCNPJ(String cnpj) {
        int[] PESO_PRIMEIRO_DIGITO = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] PESO_SEGUNDO_DIGITO = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

        if (cnpj == null) {
            return false;
        }

        String numeros = cnpj.replaceAll("[^0-9]", "");

        if (numeros.length() != 14) {
            return false;
        }

        if (numeros.matches("^(\\d)\\1{13}$")) {
            return false;
        }

        try {
            int soma = 0;
            for (int i = 0; i < 12; i++) {
                int num = Character.getNumericValue(numeros.charAt(i));
                soma += num * PESO_PRIMEIRO_DIGITO[i];
            }
            int digito1 = 11 - (soma % 11);
            int dv1 = (digito1 >= 10) ? 0 : digito1;

            soma = 0;
            for (int i = 0; i < 13; i++) {
                int num = Character.getNumericValue(numeros.charAt(i));
                soma += num * PESO_SEGUNDO_DIGITO[i];
            }
            int digito2 = 11 - (soma % 11);
            int dv2 = (digito2 >= 10) ? 0 : digito2;

            return dv1 == Character.getNumericValue(numeros.charAt(12)) &&
                    dv2 == Character.getNumericValue(numeros.charAt(13));
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
