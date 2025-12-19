package com.shopmsa.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j Circuit Breaker 설정
 * 서비스 장애 시 자동 폴백 및 복구
 */
@Configuration
public class Resilience4jConfig {

    /**
     * Circuit Breaker 기본 설정
     */
    @Bean
    Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        // 실패율이 50%를 초과하면 Circuit Open
                        .failureRateThreshold(50)
                        // Circuit이 Half-Open 상태에서 성공률 확인할 호출 수
                        .permittedNumberOfCallsInHalfOpenState(5)
                        // 최소 호출 횟수 (이 이상 호출되어야 통계 집계)
                        .minimumNumberOfCalls(10)
                        // Sliding Window 크기 (최근 10번의 호출 통계)
                        .slidingWindowSize(10)
                        // Circuit Open 상태 유지 시간
                        .waitDurationInOpenState(Duration.ofSeconds(30))
                        // 느린 호출 임계값 (3초 이상 걸리면 느린 호출로 간주)
                        .slowCallDurationThreshold(Duration.ofSeconds(3))
                        // 느린 호출 비율 임계값 (60% 이상이면 Circuit Open)
                        .slowCallRateThreshold(60)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        // 전체 타임아웃 (10초)
                        .timeoutDuration(Duration.ofSeconds(10))
                        .build())
                .build());
    }

    /**
     * Payment Service 전용 Circuit Breaker 설정
     * 결제 서비스는 더 엄격한 설정 적용
     */
    @Bean
    Customizer<ReactiveResilience4JCircuitBreakerFactory> paymentServiceCustomizer() {
        return factory -> factory.configure(builder -> builder
                .circuitBreakerConfig(CircuitBreakerConfig.custom()
                        .failureRateThreshold(40)  // 더 낮은 실패율 임계값
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .minimumNumberOfCalls(5)
                        .slidingWindowSize(10)
                        .waitDurationInOpenState(Duration.ofSeconds(60))  // 더 긴 대기 시간
                        .slowCallDurationThreshold(Duration.ofSeconds(2))  // 더 짧은 느린 호출 임계값
                        .slowCallRateThreshold(50)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5))  // 더 짧은 타임아웃
                        .build())
                .build(), "paymentCircuitBreaker");
    }
}
