package com.pulsegpt.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI pulseGptOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PulseGPT API")
                        .description("AI-Powered Audience Intelligence and Content Planning Platform REST API")
                        .version("v2.0.0")
                        .contact(new Contact()
                                .name("PulseGPT Core Team")
                                .email("dev@pulse-gpt.ai")
                                .url("https://pulse-gpt.ai"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://pulse-gpt.ai/terms")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT Bearer token to authorize protected API requests")));
    }
}
