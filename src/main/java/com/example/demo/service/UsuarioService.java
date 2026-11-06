package com.example.demo.service;

import com.example.demo.controller.dtos.request.EnderecoRequestDTO;
import com.example.demo.controller.dtos.request.LoginRequestDTO;
import com.example.demo.controller.dtos.request.TelefoneRequestDTO;
import com.example.demo.controller.dtos.request.UsuarioUpdateRequestDTO;
import com.example.demo.controller.dtos.response.EnderecoResponseDTO;
import com.example.demo.controller.dtos.response.LoginResponseDTO;
import com.example.demo.controller.dtos.response.TelefoneResponseDTO;
import com.example.demo.controller.dtos.response.UsuarioResponseDTO;
import com.example.demo.infrastructure.entity.enums.UsuarioRole;
import com.example.demo.infrastructure.entity.usuarios.Endereco;
import com.example.demo.infrastructure.entity.usuarios.Telefone;
import com.example.demo.infrastructure.entity.usuarios.Usuario;
import com.example.demo.infrastructure.exceptions.ConflictException;
import com.example.demo.infrastructure.exceptions.ResourceNotFoundException;
import com.example.demo.infrastructure.repository.EnderecoRepository;
import com.example.demo.infrastructure.repository.TelefoneRepository;
import com.example.demo.infrastructure.repository.UsuarioRepository;
import com.example.demo.infrastructure.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EnderecoRepository enderecoRepository;
    private final TelefoneRepository telefoneRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          EnderecoRepository enderecoRepository,
                          TelefoneRepository telefoneRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil) {
        this.usuarioRepository = usuarioRepository;
        this.enderecoRepository = enderecoRepository;
        this.telefoneRepository = telefoneRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    // ===================== Autenticação =====================

    public LoginResponseDTO login(LoginRequestDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.senha())
        );
        return new LoginResponseDTO(jwtUtil.generateToken(authentication.getName()), "Bearer");
    }

    // ===================== Uso interno (Cliente/Vendedor) =====================

    // Monta (sem salvar) um novo usuário com senha criptografada. Quem chama decide quando salvar.
    public Usuario novoUsuario(String nome, String email, String senha,
                               List<EnderecoRequestDTO> enderecos,
                               List<TelefoneRequestDTO> telefones,
                               UsuarioRole role) {
        emailExiste(email);

        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(senha));
        usuario.setUsuarioRole(role);
        usuario.setEnderecos(new ArrayList<>(enderecos == null ? List.of()
                : enderecos.stream().map(this::toEndereco).toList()));
        usuario.setTelefones(new ArrayList<>(telefones == null ? List.of()
                : telefones.stream().map(this::toTelefone).toList()));
        return usuario;
    }

    public void emailExiste(String email) {
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictException("Email ja cadastrado: " + email);
        }
    }

    public Usuario buscarEntidadePorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email não encontrado: " + email));
    }

    public boolean isAdmin(String email) {
        return buscarEntidadePorEmail(email).getUsuarioRole() == UsuarioRole.ADMIN;
    }

    // ===================== Perfil do usuário logado =====================

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarMeuPerfil(String email) {
        return UsuarioResponseDTO.from(buscarEntidadePorEmail(email));
    }

    @Transactional
    public UsuarioResponseDTO atualizar(String email, UsuarioUpdateRequestDTO dto) {
        Usuario usuario = buscarEntidadePorEmail(email);
        if (dto.nome() != null && !dto.nome().isBlank()) {
            usuario.setNome(dto.nome());
        }
        if (dto.senha() != null && !dto.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(dto.senha()));
        }
        return UsuarioResponseDTO.from(usuarioRepository.save(usuario));
    }

    // ===================== Endereços =====================

    @Transactional(readOnly = true)
    public List<EnderecoResponseDTO> listarEnderecos(String email) {
        return buscarEntidadePorEmail(email).getEnderecos().stream().map(EnderecoResponseDTO::from).toList();
    }

    @Transactional
    public EnderecoResponseDTO adicionarEndereco(String email, EnderecoRequestDTO dto) {
        Usuario usuario = buscarEntidadePorEmail(email);
        Endereco endereco = enderecoRepository.save(toEndereco(dto));
        usuario.getEnderecos().add(endereco);
        usuarioRepository.save(usuario);
        return EnderecoResponseDTO.from(endereco);
    }

    @Transactional
    public EnderecoResponseDTO atualizarEndereco(String email, Long enderecoId, EnderecoRequestDTO dto) {
        Endereco endereco = buscarEntidadePorEmail(email).getEnderecos().stream()
                .filter(e -> e.getId().equals(enderecoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado: " + enderecoId));
        endereco.setRua(dto.rua());
        endereco.setNumero(dto.numero());
        endereco.setComplemento(dto.complemento());
        endereco.setCidade(dto.cidade());
        endereco.setEstado(dto.estado().toUpperCase());
        endereco.setCep(dto.cep());
        return EnderecoResponseDTO.from(enderecoRepository.save(endereco));
    }

    @Transactional
    public void removerEndereco(String email, Long enderecoId) {
        Usuario usuario = buscarEntidadePorEmail(email);
        Endereco endereco = usuario.getEnderecos().stream()
                .filter(e -> e.getId().equals(enderecoId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Endereço não encontrado: " + enderecoId));
        usuario.getEnderecos().remove(endereco);
        enderecoRepository.delete(endereco);
    }

    // ===================== Telefones =====================

    @Transactional(readOnly = true)
    public List<TelefoneResponseDTO> listarTelefones(String email) {
        return buscarEntidadePorEmail(email).getTelefones().stream().map(TelefoneResponseDTO::from).toList();
    }

    @Transactional
    public TelefoneResponseDTO adicionarTelefone(String email, TelefoneRequestDTO dto) {
        Usuario usuario = buscarEntidadePorEmail(email);
        Telefone telefone = telefoneRepository.save(toTelefone(dto));
        usuario.getTelefones().add(telefone);
        usuarioRepository.save(usuario);
        return TelefoneResponseDTO.from(telefone);
    }

    @Transactional
    public void removerTelefone(String email, Long telefoneId) {
        Usuario usuario = buscarEntidadePorEmail(email);
        Telefone telefone = usuario.getTelefones().stream()
                .filter(t -> t.getId().equals(telefoneId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Telefone não encontrado: " + telefoneId));
        usuario.getTelefones().remove(telefone);
        telefoneRepository.delete(telefone);
    }

    // ===================== Administração =====================

    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarTodos() {
        return usuarioRepository.findAll().stream().map(UsuarioResponseDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarUsuarioPorEmail(String email) {
        return UsuarioResponseDTO.from(buscarEntidadePorEmail(email));
    }

    @Transactional
    public void deletarUsuarioPorEmail(String email) {
        buscarEntidadePorEmail(email); // garante 404 se não existir
        usuarioRepository.deleteByEmail(email);
    }

    // ===================== Conversões =====================

    private Endereco toEndereco(EnderecoRequestDTO dto) {
        Endereco endereco = new Endereco();
        endereco.setRua(dto.rua());
        endereco.setNumero(dto.numero());
        endereco.setComplemento(dto.complemento());
        endereco.setCidade(dto.cidade());
        endereco.setEstado(dto.estado().toUpperCase());
        endereco.setCep(dto.cep());
        return endereco;
    }

    private Telefone toTelefone(TelefoneRequestDTO dto) {
        Telefone telefone = new Telefone();
        telefone.setDdd(dto.ddd());
        telefone.setTelefone(dto.numero());
        return telefone;
    }
}
