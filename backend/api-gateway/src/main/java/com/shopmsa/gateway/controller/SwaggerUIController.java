package com.shopmsa.gateway.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import reactor.core.publisher.Mono;


@Controller
public class SwaggerUIController {
    
    @GetMapping("/v3/api-docs/user-service")
    public Mono<Void> userServiceDocs(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
        response.getHeaders().setLocation(URI.create("http://localhost:8081/v3/api-docs"));
        return response.setComplete();
    }
    
    @GetMapping("/v3/api-docs/payment-service")
    public Mono<Void> paymentServiceDocs(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
        response.getHeaders().setLocation(URI.create("http://localhost:8082/v3/api-docs"));
        return response.setComplete();
    }
}
