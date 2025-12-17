# 테스트 환경 가이드 (백엔드 개발용 인프라)

백엔드 개발 시 로컬에서 실행하면서 필요한 인프라(PostgreSQL, Redis, Kafka)만 Kubernetes에서 실행하는 가이드입니다.

## 📋 개요

테스트 환경은 **인프라 컴포넌트만** 배포하여 백엔드 개발을 지원합니다.

### 배포되는 컴포넌트
- ✅ PostgreSQL (단일 인스턴스)
- ✅ Redis Sentinel (Master + Replica 1 + Sentinel 1)
- ✅ Kafka + Zookeeper (단일 브로커)

### 배포되지 않는 컴포넌트
- ❌ API Gateway
- ❌ User Service
- ❌ Payment Service
- ❌ Settlement Service
- ❌ Partner Service
- ❌ Accounting Service

## 🚀 빠른 시작

### 1. 인프라 실행

```bash
cd k8s/scripts
./start-test-infra.sh
```

### 2. 배포 확인

```bash
# Pod 상태 확인
kubectl get pods -n shop-msa -w

# 모든 Pod가 Running 상태가 될 때까지 대기 (약 2-3분)
```

### 3. 백엔드 애플리케이션 로컬 실행

```bash
# 예: API Gateway 로컬 실행
cd api-gateway
./mvnw spring-boot:run -Dspring-boot.run.profiles=test

# 다른 터미널에서 다른 서비스 실행
cd ../user-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
```

### 4. 인프라 종료

```bash
cd k8s/scripts
./stop-test-infra.sh
```

## 🔗 연결 정보

### PostgreSQL

#### Kubernetes 내부에서 접근
```yaml
Host: postgresql.shop-msa.svc.cluster.local
Port: 5432
```

#### 로컬에서 접근 (Port Forward)
```bash
# Port Forward 시작
kubectl port-forward svc/postgresql 5432:5432 -n shop-msa

# 다른 터미널에서 접속
psql -h localhost -p 5432 -U postgres

# Spring Boot application.yml 설정
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/userdb
    username: userservice
    password: test_user_password
```

#### 데이터베이스 목록
| 데이터베이스 | 사용자 | 비밀번호 | 용도 |
|-------------|--------|----------|------|
| userdb | userservice | test_user_password | User Service |
| paymentdb | paymentservice | test_payment_password | Payment Service |
| settlementdb | settlementservice | test_settlement_password | Settlement Service |
| partnerdb | partnerservice | test_partner_password | Partner Service |
| accountingdb | accountingservice | test_accounting_password | Accounting Service |

### Redis

#### Kubernetes 내부에서 접근
```yaml
Master: redis-master.shop-msa.svc.cluster.local:6379
Sentinel: redis-sentinel.shop-msa.svc.cluster.local:26379
```

#### 로컬에서 접근 (Port Forward)
```bash
# Port Forward 시작
kubectl port-forward svc/redis-master 6379:6379 -n shop-msa

# Redis CLI 접속
redis-cli -h localhost -p 6379

# Spring Boot application.yml 설정 (Sentinel 사용 시)
spring:
  data:
    redis:
      sentinel:
        master: mymaster
        nodes:
          - localhost:26379
```

### Kafka

#### Kubernetes 내부에서 접근
```yaml
Bootstrap Servers: kafka.shop-msa.svc.cluster.local:9092
```

#### 로컬에서 접근 (Port Forward)
```bash
# Port Forward 시작
kubectl port-forward svc/kafka 9092:9092 -n shop-msa

# Spring Boot application.yml 설정
spring:
  kafka:
    bootstrap-servers: localhost:9092
```

## 📝 Spring Boot 설정 예시

### application-test.yml

```yaml
spring:
  profiles:
    active: test
  
  # PostgreSQL 설정
  datasource:
    url: jdbc:postgresql://localhost:5432/userdb
    username: userservice
    password: test_user_password
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  
  # Redis 설정 (단순 연결)
  data:
    redis:
      host: localhost
      port: 6379
  
  # Kafka 설정
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: test-consumer-group
      auto-offset-reset: earliest
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

logging:
  level:
    root: INFO
    com.payment: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
```

## 🛠️ 유용한 명령어

### Pod 관리

```bash
# Pod 상태 확인
kubectl get pods -n shop-msa

# Pod 로그 확인
kubectl logs -f postgresql-0 -n shop-msa
kubectl logs -f redis-master-0 -n shop-msa
kubectl logs -f kafka-0 -n shop-msa

# Pod 재시작
kubectl delete pod postgresql-0 -n shop-msa
```

### PostgreSQL 관리

```bash
# PostgreSQL Pod 접속
kubectl exec -it postgresql-0 -n shop-msa -- psql -U postgres

# 데이터베이스 목록 확인
\l

# 특정 데이터베이스 접속
\c userdb

# 테이블 목록 확인
\dt

# SQL 실행
SELECT * FROM users;
```

### Redis 관리

```bash
# Redis CLI 접속
kubectl exec -it redis-master-0 -n shop-msa -- redis-cli

# 키 확인
KEYS *

# 특정 키 값 확인
GET user:123

# Redis 정보 확인
INFO replication
```

### Kafka 관리

```bash
# Kafka Pod 접속
kubectl exec -it kafka-0 -n shop-msa -- bash

# 토픽 목록 확인
kafka-topics --list --bootstrap-server localhost:9092

# 토픽 생성
kafka-topics --create --topic test-topic \
  --partitions 3 --replication-factor 1 \
  --bootstrap-server localhost:9092

# 토픽 상세 정보
kafka-topics --describe --topic test-topic \
  --bootstrap-server localhost:9092

# 메시지 발행 (Producer)
kafka-console-producer --topic test-topic \
  --bootstrap-server localhost:9092

# 메시지 구독 (Consumer)
kafka-console-consumer --topic test-topic \
  --from-beginning --bootstrap-server localhost:9092
```

## 🔄 Port Forward 한번에 실행

여러 Port Forward를 동시에 실행하는 스크립트:

```bash
#!/bin/bash
# port-forward-all.sh

# 백그라운드에서 실행
kubectl port-forward svc/postgresql 5432:5432 -n shop-msa &
kubectl port-forward svc/redis-master 6379:6379 -n shop-msa &
kubectl port-forward svc/kafka 9092:9092 -n shop-msa &

echo "Port Forward 실행 중..."
echo "PostgreSQL: localhost:5432"
echo "Redis: localhost:6379"
echo "Kafka: localhost:9092"
echo ""
echo "종료하려면 Ctrl+C 또는 pkill -f 'kubectl port-forward'"

# 대기
wait
```

실행:
```bash
chmod +x port-forward-all.sh
./port-forward-all.sh
```

종료:
```bash
pkill -f 'kubectl port-forward'
```

## 📊 리소스 사용량

### 테스트 환경 리소스

| 컴포넌트 | CPU Request | Memory Request | Storage |
|----------|-------------|----------------|---------|
| PostgreSQL | 100m | 256Mi | 2Gi |
| Redis Master | 100m | 128Mi | 1Gi |
| Redis Replica | 100m | 128Mi | 1Gi |
| Redis Sentinel | 50m | 64Mi | - |
| Zookeeper | 100m | 256Mi | 2Gi |
| Kafka | 250m | 512Mi | 3Gi |
| **총합** | **700m** | **1.3Gi** | **10Gi** |

## 🐛 문제 해결

### Pod가 시작되지 않음

```bash
# Pod 상태 확인
kubectl describe pod <pod-name> -n shop-msa

# 이벤트 확인
kubectl get events -n shop-msa --sort-by='.lastTimestamp'

# 로그 확인
kubectl logs <pod-name> -n shop-msa
```

### Port Forward 연결 실패

```bash
# Port Forward 프로세스 확인
ps aux | grep 'kubectl port-forward'

# 기존 Port Forward 종료
pkill -f 'kubectl port-forward'

# 다시 시작
kubectl port-forward svc/postgresql 5432:5432 -n shop-msa
```

### 데이터 초기화

```bash
# 인프라 종료
./stop-test-infra.sh

# PVC 삭제 (데이터 완전 삭제)
kubectl delete pvc --all -n shop-msa

# 인프라 재시작
./start-test-infra.sh
```

## 💡 개발 팁

### 1. 자동 재시작 설정
Spring Boot DevTools를 사용하면 코드 변경 시 자동으로 재시작됩니다.

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <optional>true</optional>
</dependency>
```

### 2. 데이터 Seed 스크립트
`src/main/resources/data.sql`에 초기 데이터 작성:

```sql
-- data.sql
INSERT INTO users (id, name, email) VALUES 
  (1, 'Test User', 'test@example.com');
```

### 3. H2 Console 활성화
개발 중 빠른 테스트를 위해 H2 Console 사용:

```yaml
# application-test.yml
spring:
  h2:
    console:
      enabled: true
      path: /h2-console
```

## 🔒 보안 주의사항

⚠️ **테스트 환경은 개발 전용입니다!**

- 운영 환경에 절대 사용하지 마세요
- 테스트 비밀번호는 단순하게 설정되어 있습니다
- 외부 네트워크에 노출하지 마세요
- 중요한 데이터를 저장하지 마세요

## 📚 추가 참고 자료

- [Spring Boot Test 문서](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Testcontainers](https://www.testcontainers.org/) - 완전히 격리된 테스트 환경
- [Docker Compose](https://docs.docker.com/compose/) - 로컬 개발 대안

---

**Happy Coding! 🚀**
