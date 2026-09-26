package com.eduquest.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    private static final String SECURITY_SCHEME = "bearer-jwt";

    @Bean
    public OpenAPI eduquestOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("EduQuest API")
                        .description("Backend del sistema EduQuest: autenticación JWT, usuarios y roles, "
                                + "documentos académicos, grupos de estudio, reseñas de profesores y "
                                + "planes de estudio generados con IA.")
                        .version("1.0.0")
                        .contact(new Contact().name("EduQuest")))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT obtenido en POST /api/v1/auth/login. Enviar como "
                                        + "'Authorization: Bearer <token>'.")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}