package com.example.demo.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Configuração do Swagger: http://localhost:8080/swagger-ui.html
@Configuration
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI ecommerceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-commerce API")
                        .version("v1")
                        .description("""
                                API de marketplace (estilo Mercado Livre / KaBuM).

                                **Como autenticar:**
                                1. Cadastre-se em `POST /api/clientes` ou `POST /api/vendedores`
                                2. Faça login em `POST /api/usuario/login` e copie o `token`
                                3. Clique em **Authorize** e cole o token (sem o prefixo "Bearer")
                                """)
                        .contact(new Contact().name("PominiGa")))
                // Aplica o JWT em todos os endpoints por padrão (os públicos removem com @SecurityRequirements)
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .name(BEARER)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
