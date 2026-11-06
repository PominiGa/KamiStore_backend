package com.example.demo.infrastructure.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Habilita @PreAuthorize nos controllers (controle por perfil)
public class SecurityConfig {

    // Endpoints da documentação Swagger / OpenAPI
    private static final String[] SWAGGER = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    };

    // Instâncias de JwtUtil e UserDetailsService injetadas pelo Spring
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    // Construtor para injeção de dependências de JwtUtil e UserDetailsService
    @Autowired
    public SecurityConfig(JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    // Configuração do filtro de segurança
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Cria uma instância do JwtRequestFilter com JwtUtil e UserDetailsService
        JwtRequestFilter jwtRequestFilter = new JwtRequestFilter(jwtUtil, userDetailsService);

        http
                .csrf(AbstractHttpConfigurer::disable) // Desativa proteção CSRF para APIs REST (não aplicável a APIs que não mantêm estado)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(SWAGGER).permitAll() // Documentação pública
                        .requestMatchers(HttpMethod.POST, "/api/usuario/login").permitAll() // Login
                        .requestMatchers(HttpMethod.POST, "/api/clientes", "/api/vendedores").permitAll() // Cadastro
                        .requestMatchers(HttpMethod.GET, "/api/produtos/**").permitAll() // Vitrine pública de produtos e avaliações
                        .anyRequest().authenticated() // Requer autenticação para todas as outras requisições
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Configura a política de sessão como stateless (sem sessão)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) // Sem token / token inválido => 401
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class); // Adiciona o filtro JWT antes do filtro de autenticação padrão

        // Retorna a configuração do filtro de segurança construída
        return http.build();
    }

    // Configura o PasswordEncoder para criptografar senhas usando BCrypt
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Retorna uma instância de BCryptPasswordEncoder
    }

    // Configura o AuthenticationManager usando AuthenticationConfiguration
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        // Obtém e retorna o AuthenticationManager da configuração de autenticação
        return authenticationConfiguration.getAuthenticationManager();
    }

}
