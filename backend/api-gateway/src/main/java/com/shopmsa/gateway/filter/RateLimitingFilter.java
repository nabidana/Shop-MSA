package com.shopmsa.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Redis 기반 Rate Limiting 필터
 * IP 기반으로 분당 요청 수 제한
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    
    // 분당 최대 요청 수
    private static final long MAX_REQUESTS_PER_MINUTE = 100;
    
    // Rate Limit 윈도우 (1분)
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofMinutes(1);

    // Redis 연결 타임아웃 (2초)
    private static final Duration REDIS_TIMEOUT = Duration.ofSeconds(2);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String clientIp = getClientIp(request);
        String rateLimitKey = "rate_limit:" + clientIp;
        
        return redisTemplate.opsForValue()
                .increment(rateLimitKey)
                .timeout(REDIS_TIMEOUT)  // ✅ 타임아웃 2초 설정
                .flatMap(count -> {
                    // 첫 요청인 경우 TTL 설정
                    if (count == 1) {
                        return redisTemplate.expire(rateLimitKey, RATE_LIMIT_WINDOW)
                                .then(processRequest(exchange, chain, count));
                    }
                    
                    return processRequest(exchange, chain, count);
                })
                .onErrorResume(TimeoutException.class, error -> {
                    // ✅ 타임아웃 발생 시
                    log.warn("Rate limiting timeout for IP: {} - allowing request", clientIp);
                    return chain.filter(exchange);
                })
                .onErrorResume(error -> {
                    // Redis 오류 시 요청 허용 (Fail-Open)
                    log.error("Rate limiting error: {}", error.getMessage());
                    return chain.filter(exchange);
                });
    }

    private Mono<Void> processRequest(ServerWebExchange exchange, GatewayFilterChain chain, Long count) {
        ServerHttpResponse response = exchange.getResponse();
        // ✅ beforeCommit에서 Response Header 추가
        response.beforeCommit(() -> {
            response.getHeaders().add("X-Rate-Limit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
            response.getHeaders().add("X-Rate-Limit-Remaining", 
                    String.valueOf(Math.max(0, MAX_REQUESTS_PER_MINUTE - count)));
            return Mono.empty();
        });

        // Response 헤더에 Rate Limit 정보 추가
        // response.getHeaders().add("X-Rate-Limit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
        // response.getHeaders().add("X-Rate-Limit-Remaining", 
        //         String.valueOf(Math.max(0, MAX_REQUESTS_PER_MINUTE - count)));
        
        // Rate Limit 초과 시 429 응답
        if (count > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for IP: {} (count: {})", 
                    getClientIp(exchange.getRequest()), count);
            
            response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            response.getHeaders().add("X-Rate-Limit-Retry-After", "60");
            return response.setComplete();
        }
        
        return chain.filter(exchange);
    }

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
        return Ordered.HIGHEST_PRECEDENCE + 1;  // 로깅 다음에 실행
    }
}
