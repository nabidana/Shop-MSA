# Spring Boot 에러 해결
### 1. Maven Dependencies
> SpringBoot 4.0.1 버전 사용으로 인해, 모든 maven 라이브러리를 SpringBoot 4.0.1 버전에 맞춰서 작동해야하지만, 핵심 라이브러리 중 하나인<br/>
### spring-cloud-starter-gateway</br>
> 라이브러리가 이전 Spring 버전을 참조하고 있어서 Boot & Cloud와 호환성 문제 발생

### 해결 방안
> 해당 라이브러리가 사용하는 Dependencies를 직접 최신버전으로 찾아서 명시

### 2. Redis Config Bean
> 에러
```log
springboot required a single bean, but 2 were found
```
> SpringBoot의 라이브러리에 선언된 bean을 Config 에서 overide 시, 두개 이상의 bean을 설정해서 발생하는문제
### 해결 방안
> ReactiveStringRedisTemplate class에서 선언된 선언자로 변경<br/>
```java
public ReactiveStringRedisTemplate(ReactiveRedisConnectionFactory connectionFactory,
			RedisSerializationContext<String, String> serializationContext) {
		super(connectionFactory, serializationContext);
	}
```

### 3. Redis connection refused 문제 발생
> 에러
```log
Rate limiting error: Unable to connect to Redis
```
> Redis Sentinal 연결은 정상 이나, Redis Sentinal 연결 후 Master와의 연결에서 문제 발생
> - Sentinel localhost:26379 → Master IP 10.244.2.1:6379 반환
> - 10.244.2.1:6379 <- Kubernetes Pod의 내부 IP
> - 로컬 머신에서는 Kubernetes Pod IP(10.244.x.x)에 직접 접근할 수 없음

### 해결 방안
> Ubuntu의 iptables NAT 규칙 이용하여 10.244.2.1:6379 → 127.0.0.1:6379 자동 변환
```bash
# Master Pod IP 확인
MASTER_IP=$(kubectl get pod -n shop-msa redis-master-0 -o jsonpath='{.status.podIP}')
echo "Master IP: $MASTER_IP"
# iptables NAT 규칙 추가 (sudo 필요)
sudo iptables -t nat -A OUTPUT -d $MASTER_IP -p tcp --dport 6379 -j DNAT --to-destination 127.0.0.1:6379
# 확인
sudo iptables -t nat -L OUTPUT -n -v | grep $MASTER_IP
```