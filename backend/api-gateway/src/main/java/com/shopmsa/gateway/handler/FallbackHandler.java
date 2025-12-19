package com.shopmsa.gateway.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.shopmsa.gateway.dto.ErrorResponse;

import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Circuit Breaker Fallback 핸들러
 * 서비스 장애 시 기본 응답 제공
 */
@Slf4j
@Component
public class FallbackHandler {

    /**
     * Payment Service Fallback
     * 결제 서비스 장애 시 응답
     */
    public Mono<ServerResponse> paymentServiceFallback(ServerRequest request) {
        log.error("Payment service is unavailable. Fallback triggered for request: {}", 
                request.path());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("결제 서비스가 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.")
                .path(request.path())
                .build();
        
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(errorResponse);
    }

    /**
     * User Service Fallback
     */
    public Mono<ServerResponse> userServiceFallback(ServerRequest request) {
        log.error("User service is unavailable. Fallback triggered for request: {}", 
                request.path());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("사용자 서비스가 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.")
                .path(request.path())
                .build();
        
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(errorResponse);
    }

    /**
     * Settlement Service Fallback
     */
    public Mono<ServerResponse> settlementServiceFallback(ServerRequest request) {
        log.error("Settlement service is unavailable. Fallback triggered for request: {}", 
                request.path());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("정산 서비스가 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.")
                .path(request.path())
                .build();
        
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(errorResponse);
    }

    /**
     * Partner Service Fallback
     */
    public Mono<ServerResponse> partnerServiceFallback(ServerRequest request) {
        log.error("Partner service is unavailable. Fallback triggered for request: {}", 
                request.path());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("파트너 서비스가 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.")
                .path(request.path())
                .build();
        
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(errorResponse);
    }

    /**
     * Accounting Service Fallback
     */
    public Mono<ServerResponse> accountingServiceFallback(ServerRequest request) {
        log.error("Accounting service is unavailable. Fallback triggered for request: {}", 
                request.path());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("회계 서비스가 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.")
                .path(request.path())
                .build();
        
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(errorResponse);
    }

    /**
     * 기본 Fallback (알 수 없는 서비스)
     */
    public Mono<ServerResponse> defaultFallback(ServerRequest request) {
        log.error("Unknown service is unavailable. Fallback triggered for request: {}", 
                request.path());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message("요청하신 서비스가 일시적으로 사용할 수 없습니다. 잠시 후 다시 시도해주세요.")
                .path(request.path())
                .build();
        
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(errorResponse);
    }
}
