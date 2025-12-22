package com.shopmsa.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis Sentinel 설정
 * Rate Limiting, Session 관리, Cache에 사용
 */
@Slf4j
@Configuration
// @RequiredArgsConstructor
public class RedisConfig {

    // @Value("${spring.data.redis.sentinel.master}")
    // private String sentinelMaster;
    // private String sentinelMaster = "mymaster";

    // @Value("${spring.data.redis.sentinel.nodes}")
    // private List<String> sentinelNodes;
    // private String sentinelNodes = "localhost:26379";

    // @Value("${spring.data.redis.password:}")
    // private String password;
    // private final RedisSentinelProperties sentinelProperties;

    // DataRedisReactiveAutoConfiguration
    // RedisSentinelConfiguration
    // LettuceConnectionConfiguration
    /**
     * Redis Sentinel 연결 팩토리
     * https://docs.spring.io/spring-data/redis/reference/redis/connection-modes.html
     */
    // @Bean
    // RedisConnectionFactory redisConnectionFactory() {
    //     RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
    //     .master("mymaster")
    //     .sentinel("localhost", 26379);
    //     log.info("Redis ConnectionFactory initialized successfully");

    //     return new LettuceConnectionFactory(sentinelConfig);
    // }
    // @Bean
    // @ConditionalOnMissingBean(ReactiveRedisConnectionFactory.class)
    // LettuceConnectionFactory reactiveRedisConnectionFactory() {
    //     log.info("Configuring Redis Sentinel: master={}, nodes={}", 
    //         sentinelProperties.getMaster(), 
    //         sentinelProperties.getNodes()
    //     );
    //     RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
    //             // .master(sentinelMaster);
    //             .master(sentinelProperties.getMaster());
        
    //     // Sentinel 노드 파싱 및 추가
    //     // sentinelNodes
    //     sentinelProperties.getNodes()
    //     .forEach(node -> {
    //         String[] parts = node.split(":");
    //         String host = parts[0];
    //         int port = Integer.parseInt(parts[1]);
    //         sentinelConfig.sentinel(host, port);
    //         log.info("Added Sentinel node: {}:{}", host, port);
    //     });
    //     // Set<String> sentinelHostAndPorts = Arrays.stream(sentinelNodes.split(","))
    //     //         .map(String::trim)
    //     //         .collect(Collectors.toSet());
        
    //     // for (String hostAndPort : sentinelHostAndPorts) {
    //     //     String[] parts = hostAndPort.split(":");
    //     //     sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
    //     // }
        
    //     // 비밀번호 설정 (있는 경우)
    //     // if (password != null && !password.isEmpty()) {
    //     //     sentinelConfig.setPassword(password);
    //     // }
    //     LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
    //         .commandTimeout(Duration.ofSeconds(3))
    //         .build();
        
    //     LettuceConnectionFactory factory = new LettuceConnectionFactory(sentinelConfig, clientConfig);
    //     factory.afterPropertiesSet();
        
    //     log.info("Redis ConnectionFactory initialized successfully");
    //     // return new LettuceConnectionFactory(sentinelConfig);
    //     return factory;
    // }

    /**
     * Reactive Redis Template
     * String 키/값 직렬화 사용
     * ReactiveRedisTemplate
     * ReactiveStringRedisTemplate
     */
    @Bean
    ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory connectionFactory
    ) {
        RedisSerializationContext<String, String> serializationContext = 
            RedisSerializationContext.<String, String>newSerializationContext(new StringRedisSerializer())
                .key(new StringRedisSerializer())
                .value(new StringRedisSerializer())
                .hashKey(new StringRedisSerializer())
                .hashValue(new StringRedisSerializer())
                .build();
        
        log.info("ReactiveRedisTemplate initialized");
        return new ReactiveStringRedisTemplate(connectionFactory, serializationContext);
        // return new ReactiveRedisTemplate<>(connectionFactory, RedisSerializationContext.string());
    }
}
