package com.shopmsa.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.shopmsa.gateway.handler.FallbackHandler;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;

/**
 * Fallback Router 설정
 * Circuit Breaker Fallback 경로 정의
 */
@Configuration
@RequiredArgsConstructor
public class FallbackRouterConfig {

    private final FallbackHandler fallbackHandler;

    @Bean
    RouterFunction<ServerResponse> fallbackRoutes() {
        return RouterFunctions
                // Payment Service Fallback
                .route(GET("/fallback/payment"), fallbackHandler::paymentServiceFallback)
                .andRoute(POST("/fallback/payment"), fallbackHandler::paymentServiceFallback)
                
                // User Service Fallback
                .andRoute(GET("/fallback/user"), fallbackHandler::userServiceFallback)
                .andRoute(POST("/fallback/user"), fallbackHandler::userServiceFallback)
                
                // Settlement Service Fallback
                .andRoute(GET("/fallback/settlement"), fallbackHandler::settlementServiceFallback)
                .andRoute(POST("/fallback/settlement"), fallbackHandler::settlementServiceFallback)
                
                // Partner Service Fallback
                .andRoute(GET("/fallback/partner"), fallbackHandler::partnerServiceFallback)
                .andRoute(POST("/fallback/partner"), fallbackHandler::partnerServiceFallback)
                
                // Accounting Service Fallback
                .andRoute(GET("/fallback/accounting"), fallbackHandler::accountingServiceFallback)
                .andRoute(POST("/fallback/accounting"), fallbackHandler::accountingServiceFallback)
                
                // Default Fallback
                .andRoute(GET("/fallback/default"), fallbackHandler::defaultFallback)
                .andRoute(POST("/fallback/default"), fallbackHandler::defaultFallback);
    }
}
