package com.shopmsa.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) 설정
 * 웹 브라우저에서 API Gateway에 접근할 수 있도록 설정
 */
@Configuration
public class CorsConfig {

    @Bean
    CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // 허용할 Origin (개발/운영 환경별로 다르게 설정)
        corsConfig.setAllowedOriginPatterns(List.of("*"));
        
        // 허용할 HTTP 메서드
        corsConfig.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        // 허용할 헤더
        corsConfig.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "X-Request-ID",
                "X-Correlation-ID"
        ));
        
        // 노출할 헤더 (클라이언트가 읽을 수 있는 헤더)
        corsConfig.setExposedHeaders(Arrays.asList(
                "X-Request-ID",
                "X-Correlation-ID",
                "X-Rate-Limit-Remaining",
                "X-Rate-Limit-Reset"
        ));
        
        // 인증 정보 포함 허용
        corsConfig.setAllowCredentials(true);
        
        // preflight 요청 캐시 시간 (1시간)
        corsConfig.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}
