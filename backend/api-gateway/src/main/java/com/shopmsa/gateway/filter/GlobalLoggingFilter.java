package com.shopmsa.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.UUID;

/**
 * 글로벌 로깅 필터
 * 모든 요청/응답을 로깅하고 Request ID 추가
 */
@Slf4j
@Component
public class GlobalLoggingFilter implements GlobalFilter, Ordered {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Request ID 생성 또는 기존 ID 사용
        final String requestId = Optional.ofNullable(request.getHeaders().getFirst(REQUEST_ID_HEADER))
                .filter(id -> !id.isEmpty())
                .orElse(UUID.randomUUID().toString());
        request.getHeaders().getFirst(REQUEST_ID_HEADER);
        // if (requestId == null || requestId.isEmpty()) {
        //     // effectively final 위반 함
        //     // 재할당이 불가능함.
        //     requestId = UUID.randomUUID().toString();
        // }
        // Correlation ID 생성 또는 기존 ID 사용
        final String correlationId = Optional.ofNullable(request.getHeaders().getFirst(CORRELATION_ID_HEADER))
                .filter(id -> !id.isEmpty())
                .orElse(UUID.randomUUID().toString());
        // String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        // if (correlationId == null || correlationId.isEmpty()) {
        //     correlationId = UUID.randomUUID().toString();
        // }
        
        // Request에 ID 추가
        ServerHttpRequest mutatedRequest = request.mutate()
                .header(REQUEST_ID_HEADER, requestId)
                .header(CORRELATION_ID_HEADER, correlationId)
                .build();
        
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(mutatedRequest)
                .build();
        
        // 요청 로깅
        long startTime = System.currentTimeMillis();
        log.info(">>> Incoming Request: {} {} | Request-ID: {} | Correlation-ID: {} | Client-IP: {}",
                request.getMethod(),
                request.getURI(),
                requestId,
                correlationId,
                getClientIp(request));
        
        // Response에도 ID 추가 및 응답 로깅
        return chain.filter(mutatedExchange).then(Mono.fromRunnable(() -> {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            mutatedExchange.getResponse().getHeaders().add(REQUEST_ID_HEADER, requestId);
            mutatedExchange.getResponse().getHeaders().add(CORRELATION_ID_HEADER, correlationId);
            
            log.info("<<< Outgoing Response: {} {} | Status: {} | Duration: {}ms | Request-ID: {}",
                    request.getMethod(),
                    request.getURI(),
                    mutatedExchange.getResponse().getStatusCode(),
                    duration,
                    requestId);
        }));
    }

    /**
     * 클라이언트 IP 추출
     * X-Forwarded-For, X-Real-IP 헤더 확인 후 Remote Address 사용
     */
    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null 
                ? request.getRemoteAddress().getAddress().getHostAddress() 
                : "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;  // 가장 먼저 실행
    }
}
