package com.shopmsa.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis Sentinel 설정
 * Rate Limiting, Session 관리, Cache에 사용
 */
@Configuration
public class RedisConfig {

    // @Value("${spring.data.redis.sentinel.master:mymaster}")
    private String sentinelMaster = "mymaster";

    // @Value("${spring.data.redis.sentinel.nodes:localhost:26379}")
    private String sentinelNodes = "localhost:26379";

    // @Value("${spring.data.redis.password:}")
    private String password;

    /**
     * Redis Sentinel 연결 팩토리
     */
    // @Bean
    // ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
    //     RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
    //             .master(sentinelMaster);
        
    //     // Sentinel 노드 파싱 및 추가
    //     Set<String> sentinelHostAndPorts = Arrays.stream(sentinelNodes.split(","))
    //             .map(String::trim)
    //             .collect(Collectors.toSet());
        
    //     for (String hostAndPort : sentinelHostAndPorts) {
    //         String[] parts = hostAndPort.split(":");
    //         sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
    //     }
        
    //     // 비밀번호 설정 (있는 경우)
    //     if (password != null && !password.isEmpty()) {
    //         sentinelConfig.setPassword(password);
    //     }
        
    //     return new LettuceConnectionFactory(sentinelConfig);
    // }

    // /**
    //  * Reactive Redis Template
    //  * String 키/값 직렬화 사용
    //  */
    // @Bean
    // ReactiveStringRedisTemplate reactiveStringRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        
    //     StringRedisSerializer serializer = new StringRedisSerializer();
        
    //     RedisSerializationContext<String, String> serializationContext = 
    //             RedisSerializationContext.<String, String>newSerializationContext()
    //                     .key(serializer)
    //                     .value(serializer)
    //                     .hashKey(serializer)
    //                     .hashValue(serializer)
    //                     .build();
        
    //     return new ReactiveStringRedisTemplate(connectionFactory, serializationContext);
    // }
}
