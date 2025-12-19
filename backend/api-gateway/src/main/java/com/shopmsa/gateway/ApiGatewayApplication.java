package com.shopmsa.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

/**
 * API Gateway Application
 * Spring Cloud Gateway를 사용한 마이크로서비스 API Gateway
 * 
 * @author SHOP MSA
 * @version 1.0.0
 */
@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}

	/**
	* 프로그래밍 방식으로 라우트 정의 (선택사항)
	* application.yml에서 정의하는 것을 권장하지만,
	* 복잡한 로직이 필요한 경우 여기서 정의 가능
	*/
	// @Bean
    // RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
    //     return builder.routes()
    //             // User Service Route
    //             .route("user-service", r -> r
    //                     .path("/api/users/**")
    //                     .filters(f -> f
    //                             .stripPrefix(2)  // /api/users -> /
    //                             .retry(config -> config.setRetries(3)))
    //                     .uri("http://user-service:8081"))
                
    //             // Payment Service Route (중요 서비스 - 더 많은 필터 적용)
    //             .route("payment-service", r -> r
    //                     .path("/api/payments/**")
    //                     .filters(f -> f
    //                             .stripPrefix(2)
    //                             .circuitBreaker(config -> config
    //                                     .setName("paymentCircuitBreaker")
    //                                     .setFallbackUri("forward:/fallback/payment"))
    //                             .retry(config -> config.setRetries(3)))
    //                     .uri("http://payment-service:8082"))
                
    //             // Settlement Service Route
    //             .route("settlement-service", r -> r
    //                     .path("/api/settlements/**")
    //                     .filters(f -> f
    //                             .stripPrefix(2)
    //                             .retry(config -> config.setRetries(2)))
    //                     .uri("http://settlement-service:8083"))
                
    //             // Partner Service Route
    //             .route("partner-service", r -> r
    //                     .path("/api/partners/**")
    //                     .filters(f -> f
    //                             .stripPrefix(2)
    //                             .retry(config -> config.setRetries(2)))
    //                     .uri("http://partner-service:8084"))
                
    //             // Accounting Service Route
    //             .route("accounting-service", r -> r
    //                     .path("/api/accounting/**")
    //                     .filters(f -> f
    //                             .stripPrefix(2)
    //                             .retry(config -> config.setRetries(2)))
    //                     .uri("http://accounting-service:8085"))
                
    //             .build();
    // }
}
