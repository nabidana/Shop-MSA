# User Service

사용자 관리를 담당하는 마이크로서비스입니다. 회원 가입, 조회, 수정, 삭제 기능을 제공하며, Redis 캐싱과 Kafka 이벤트 발행을 지원합니다.

## 📋 목차

- [기술 스택](#기술-스택)
- [주요 기능](#주요-기능)
- [프로젝트 구조](#프로젝트-구조)
- [환경 설정](#환경-설정)
- [실행 방법](#실행-방법)
- [API 문서](#api-문서)
- [데이터베이스](#데이터베이스)
- [캐싱 전략](#캐싱-전략)
- [이벤트 발행](#이벤트-발행)
- [모니터링](#모니터링)
- [트러블슈팅](#트러블슈팅)

## 🛠 기술 스택

- **Language**: Java 21
- **Framework**: Spring Boot 3.4.1
- **Build Tool**: Maven
- **Database**: PostgreSQL 15
- **Cache**: Redis 7.2
- **Message Queue**: Kafka 3.5
- **ORM**: Spring Data JPA (Hibernate)

### 주요 의존성
```xml
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-data-redis
- spring-kafka
- postgresql
- lombok
- spring-boot-starter-validation
- spring-boot-starter-actuator
```

## ✨ 주요 기능

### 사용자 관리
- ✅ 회원 가입 (중복 체크)
- ✅ 사용자 조회 (ID, Username)
- ✅ 전체 사용자 목록 조회
- ✅ 사용자 정보 수정
- ✅ 사용자 삭제 (Soft Delete)

### 부가 기능
- 🚀 Redis 캐싱 (조회 성능 향상)
- 📨 Kafka 이벤트 발행 (생성/수정/삭제)
- 📊 Actuator Health Check
- 🔍 API 로깅

## 📁 프로젝트 구조
```
user-service/
├── src/
│   ├── main/
│   │   ├── java/com/shop/user/
│   │   │   ├── UserServiceApplication.java
│   │   │   ├── controller/
│   │   │   │   └── UserController.java
│   │   │   ├── service/
│   │   │   │   ├── UserService.java
│   │   │   │   └── UserServiceImpl.java
│   │   │   ├── repository/
│   │   │   │   └── UserRepository.java
│   │   │   ├── entity/
│   │   │   │   └── User.java
│   │   │   ├── dto/
│   │   │   │   ├── UserRequest.java
│   │   │   │   └── UserResponse.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── UserNotFoundException.java
│   │   │   │   └── ErrorResponse.java
│   │   │   └── config/
│   │   │       ├── RedisConfig.java
│   │   │       └── KafkaConfig.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-test.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── test/
│       └── java/com/shop/user/
│           └── UserServiceIntegrationTest.java
├── pom.xml
└── README.md
```

## ⚙️ 환경 설정

### 프로파일

| 프로파일 | 용도 | 데이터베이스 | Redis | Kafka |
|---------|------|------------|-------|-------|
| **test** | 로컬 테스트 | localhost:5432 | localhost:6379 | localhost:9092 |
| **dev** | 개발 환경 | Kubernetes | Kubernetes | Kubernetes |
| **prod** | 운영 환경 | Kubernetes | Kubernetes | Kubernetes |

### 환경 변수
```bash
# 데이터베이스
DB_PASSWORD=your_password

# 포트
SERVER_PORT=8081
```

## 🚀 실행 방법

### 1. 로컬 실행 (test 프로파일)

#### Prerequisites
```bash
# Port Forward로 인프라 연결
kubectl port-forward svc/postgresql 5432:5432 -n shop-msa &
kubectl port-forward svc/redis-master 6379:6379 -n shop-msa &
kubectl port-forward svc/kafka 9092:9092 -n shop-msa &
```

#### 실행
```bash
# Maven 실행
./mvnw spring-boot:run -Dspring-boot.run.profiles=test

# 또는 JAR 빌드 후 실행
./mvnw clean package
java -jar target/user-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=test
```

### 2. 개발 환경 실행 (dev 프로파일)
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. 운영 환경 실행 (prod 프로파일)
```bash
export DB_PASSWORD=secure_password
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### 4. Docker 실행
```bash
# Docker 이미지 빌드
docker build -t user-service:latest .

# Docker 컨테이너 실행
docker run -p 8081:8081 \
  -e SPRING_PROFILES_ACTIVE=test \
  -e DB_PASSWORD=password \
  user-service:latest
```

## 📚 API 문서

### Base URL
```
http://localhost:8081
```

### Endpoints

#### 1. 사용자 생성
```http
POST /api/users
Content-Type: application/json

{
  "username": "testuser",
  "email": "test@example.com",
  "password": "password123",
  "phoneNumber": "010-1234-5678"
}
```

**Response (201 Created)**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "phoneNumber": "010-1234-5678",
  "status": "ACTIVE",
  "createdAt": "2025-12-18T10:00:00",
  "updatedAt": "2025-12-18T10:00:00"
}
```

#### 2. 사용자 조회 (ID)
```http
GET /api/users/{id}
```

**Response (200 OK)**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "phoneNumber": "010-1234-5678",
  "status": "ACTIVE",
  "createdAt": "2025-12-18T10:00:00",
  "updatedAt": "2025-12-18T10:00:00"
}
```

#### 3. 사용자 조회 (Username)
```http
GET /api/users/username/{username}
```

#### 4. 전체 사용자 조회
```http
GET /api/users
```

**Response (200 OK)**
```json
[
  {
    "id": 1,
    "username": "testuser",
    "email": "test@example.com",
    "phoneNumber": "010-1234-5678",
    "status": "ACTIVE",
    "createdAt": "2025-12-18T10:00:00",
    "updatedAt": "2025-12-18T10:00:00"
  }
]
```

#### 5. 사용자 수정
```http
PUT /api/users/{id}
Content-Type: application/json

{
  "username": "testuser",
  "email": "newemail@example.com",
  "password": "newpassword123",
  "phoneNumber": "010-9876-5432"
}
```

#### 6. 사용자 삭제
```http
DELETE /api/users/{id}
```

**Response (204 No Content)**

#### 7. Health Check
```http
GET /api/users/health
```

**Response (200 OK)**
```
User Service is healthy
```

### 에러 응답

#### 404 Not Found
```json
{
  "timestamp": "2025-12-18T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found: 999",
  "path": "/api/users/999"
}
```

#### 400 Bad Request (Validation)
```json
{
  "timestamp": "2025-12-18T10:00:00",
  "status": 400,
  "error": "Validation Failed",
  "errors": {
    "username": "Username must be between 3 and 50 characters",
    "email": "Invalid email format"
  },
  "path": "/api/users"
}
```

## 🗄️ 데이터베이스

### 스키마

**Table: users**

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| id | BIGSERIAL | PRIMARY KEY | 사용자 ID |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 사용자명 |
| email | VARCHAR(100) | UNIQUE, NOT NULL | 이메일 |
| password | VARCHAR(255) | NOT NULL | 비밀번호 (암호화 필요) |
| phone_number | VARCHAR(20) | | 전화번호 |
| status | VARCHAR(20) | NOT NULL | 상태 (ACTIVE/INACTIVE/SUSPENDED/DELETED) |
| created_at | TIMESTAMP | NOT NULL | 생성일시 |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 |

### 인덱스
```sql
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
```

### 데이터베이스 연결 정보

**Test 환경**
```yaml
url: jdbc:postgresql://localhost:5432/userdb
username: userservice
password: test_user_password
```

**Dev/Prod 환경**
```yaml
url: jdbc:postgresql://postgresql.shop-msa.svc.cluster.local:5432/userdb
username: userservice
password: ${DB_PASSWORD}
```

## 💾 캐싱 전략

### Redis 캐시 설정

- **TTL**: 10분
- **캐시 키**: `users::{id}`, `users::{username}`
- **캐시 무효화**: 생성/수정/삭제 시 자동

### 캐시 적용 메서드
```java
// 캐시 저장
@Cacheable(value = "users", key = "#id")
public UserResponse getUserById(Long id) { ... }

// 캐시 무효화
@CacheEvict(value = "users", allEntries = true)
public UserResponse createUser(UserRequest request) { ... }
```

### 캐시 확인
```bash
# Redis CLI 접속
kubectl exec -it redis-master-0 -n shop-msa -- redis-cli

# 캐시 키 확인
KEYS users::*

# 캐시 값 확인
GET users::1
```

## 📨 이벤트 발행

### Kafka 토픽: `user-events`

#### 이벤트 타입

| 이벤트 | 메시지 형식 | 발행 시점 |
|--------|------------|----------|
| **USER_CREATED** | `USER_CREATED:{userId}` | 사용자 생성 시 |
| **USER_UPDATED** | `USER_UPDATED:{userId}` | 사용자 수정 시 |
| **USER_DELETED** | `USER_DELETED:{userId}` | 사용자 삭제 시 |

#### 예시
```java
// 사용자 생성 이벤트
kafkaTemplate.send("user-events", "USER_CREATED:1");

// 사용자 수정 이벤트
kafkaTemplate.send("user-events", "USER_UPDATED:1");

// 사용자 삭제 이벤트
kafkaTemplate.send("user-events", "USER_DELETED:1");
```

#### 이벤트 확인
```bash
# Kafka Consumer로 이벤트 확인
kubectl exec -it kafka-0 -n shop-msa -- \
  kafka-console-consumer \
  --topic user-events \
  --from-beginning \
  --bootstrap-server localhost:9092
```

## 📊 모니터링

### Actuator Endpoints
```bash
# Health Check
curl http://localhost:8081/actuator/health

# Metrics
curl http://localhost:8081/actuator/metrics

# Info
curl http://localhost:8081/actuator/info

# Prometheus Metrics
curl http://localhost:8081/actuator/prometheus
```

### Health Check 응답
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.2.0"
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### 로그 레벨
```yaml
# test/dev
logging:
  level:
    root: INFO
    com.shop.user: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG

# prod
logging:
  level:
    root: WARN
    com.shop.user: INFO
```

## 🐛 트러블슈팅

### 1. 데이터베이스 연결 실패

**증상**
```
Connection refused: postgresql.shop-msa.svc.cluster.local:5432
```

**해결**
```bash
# Port Forward 확인
kubectl port-forward svc/postgresql 5432:5432 -n shop-msa

# 또는 데이터베이스 상태 확인
kubectl exec -it postgresql-0 -n shop-msa -- psql -U postgres -c "SELECT 1"
```

### 2. Redis 연결 실패

**증상**
```
Could not connect to Redis at localhost:6379
```

**해결**
```bash
# Redis 상태 확인
kubectl exec -it redis-master-0 -n shop-msa -- redis-cli ping

# Port Forward 확인
kubectl port-forward svc/redis-master 6379:6379 -n shop-msa
```

### 3. Kafka 연결 실패

**증상**
```
Failed to send message to topic user-events
```

**해결**
```bash
# Kafka 상태 확인
kubectl exec -it kafka-0 -n shop-msa -- \
  kafka-topics --list --bootstrap-server localhost:9092

# Port Forward 확인
kubectl port-forward svc/kafka 9092:9092 -n shop-msa
```

### 4. 중복 사용자 생성 에러

**증상**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Username already exists"
}
```

**원인**: 동일한 username 또는 email 존재

**해결**: 다른 username/email 사용

### 5. 캐시 동기화 문제

**증상**: 데이터 수정 후에도 이전 데이터 조회

**해결**
```bash
# Redis 캐시 수동 삭제
kubectl exec -it redis-master-0 -n shop-msa -- redis-cli
> FLUSHDB

# 또는 특정 키만 삭제
> DEL users::1
```

## 🧪 테스트

### 단위 테스트 실행
```bash
./mvnw test
```

### 통합 테스트 실행
```bash
./mvnw verify
```

### 전체 빌드 및 테스트
```bash
./mvnw clean install
```

### 테스트 커버리지
```bash
./mvnw jacoco:report
# 결과: target/site/jacoco/index.html
```

## 📦 빌드 및 배포

### JAR 빌드
```bash
./mvnw clean package
# 결과: target/user-service-0.0.1-SNAPSHOT.jar
```

### Docker 이미지 빌드
```bash
docker build -t user-service:0.0.1 .
docker tag user-service:0.0.1 your-registry/user-service:0.0.1
docker push your-registry/user-service:0.0.1
```

### Kubernetes 배포
```bash
kubectl apply -f k8s/user-service-deployment.yaml
kubectl apply -f k8s/user-service-service.yaml
```

## 🔒 보안 고려사항

### TODO
- [ ] 비밀번호 암호화 (BCrypt)
- [ ] JWT 인증 구현
- [ ] API Rate Limiting
- [ ] Input Validation 강화
- [ ] SQL Injection 방지
- [ ] XSS 방지

### 권장사항
- 운영 환경에서는 HTTPS 사용
- 민감한 정보는 환경 변수로 관리
- 정기적인 보안 패치 적용