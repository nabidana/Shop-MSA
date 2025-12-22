package com.shopmsa.user.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import lombok.RequiredArgsConstructor;

@OpenAPIDefinition
@Configuration
public class SwaggerConfig {
    
    @Bean
    OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("User Service API")
                        .description("사용자 관리 API")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Shop MSA Team")
                                .email("contact@shopmsa.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("Local Server"),
                        // new Server().url("http://localhost:8080").description("Gateway Server")
                        new Server().url("/").description("Gateway Server")
                ));
    }
}
