package com.example.demo.infrastructure.security;

import com.example.demo.infrastructure.entity.Usuario;
import com.example.demo.infrastructure.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    // Repositório para acessar dados de usuário no banco de dados
    @Autowired
    private UsuarioRepository usuarioRepository;

    // Implementação do método para carregar detalhes do usuário pelo e-mail
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        boolean existe = usuarioRepository.existsByEmail(email);

        if (existe) {
            Usuario usuario = usuarioRepository.findByEmail(email);
            return org.springframework.security.core.userdetails.User
                    .withUsername(usuario.getEmail()) // Define o nome de usuário como o e-mail
                    .password(usuario.getSenha()) // Define a senha do usuário
                    .build(); // Constrói o objeto UserDetails
        } else {
            throw new UsernameNotFoundException("Usuario não encontrado: " + email);
        }
    }
}
