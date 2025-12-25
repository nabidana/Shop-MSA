package com.shopmsa.payment.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class SwaggerConfig {
    @Bean
    OpenAPI accountingServiceAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Accounting Service API")
                        .description("결제 관리 API")
                        .version("1.0.0"))
                .servers(List.of(
                        new Server().url("http://localhost:8082").description("Local Server"),
                        new Server().url("http://localhost:8080").description("Gateway Server")
                ));
    }
}
