package br.com.sicredi.api_technical_test.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Biblioteca API",
                version = "1.0",
                description = "Microsserviço de gerenciamento de Biblioteca de Livros",
                contact = @Contact(name = "Sicredi", email = "dev@sicredi.com.br")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Insira o token JWT obtido em POST /auth/login"
)
public class OpenApiConfig {}
