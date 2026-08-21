package pe.com.proveperu.sgc.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI proveperuOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("SGC PROVEPERU API")
                .description("API REST para la gestión comercial de PROVEPERU")
                .version("v1")
                .contact(new Contact().name("PROVEPERU")))
            .components(new Components().addSecuritySchemes(
                BEARER_AUTH,
                new SecurityScheme()
                    .name(BEARER_AUTH)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Ingrese únicamente el token JWT, sin escribir la palabra Bearer")
            ));
    }
}
