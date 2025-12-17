# Kubernetes 마이크로서비스 아키텍처

결제 시스템 기반의 마이크로서비스 아키텍처를 Kubernetes에 배포하기 위한 프로젝트입니다.

## 📋 목차

- [아키텍처 개요](#아키텍처-개요)
- [기술 스택](#기술-스택)
- [디렉토리 구조](#디렉토리-구조)
- [사전 요구사항](#사전-요구사항)
- [빠른 시작](#빠른-시작)
- [배포 가이드](#배포-가이드)
- [운영 가이드](#운영-가이드)
- [문제 해결](#문제-해결)

## 🏗️ 아키텍처 개요

```
아키텍처:
├── api-gateway (Spring Cloud Gateway) - 8080
├── user-service (회원 관리) - 8081
├── payment-service (결제 처리) ⭐ - 8082
├── settlement-service (정산 처리) ⭐ - 8083
├── partner-service (파트너사 관리) - 8084
└── accounting-service (회계 처리) ⭐ - 8085

인프라:
├── PostgreSQL (PostgreSQL Operator - 고가용성 클러스터)
├── Redis (Sentinel 구조 - Master/Replica)
└── Kafka (3 brokers + Zookeeper ensemble)
```

## 🛠️ 기술 스택

### 애플리케이션
- **API Gateway**: Spring Cloud Gateway
- **Backend Services**: Spring Boot
- **Build Tool**: Gradle/Maven

### 인프라
- **Container Orchestration**: Kubernetes
- **Configuration Management**: Kustomize
- **Database**: PostgreSQL (with Operator)
- **Cache**: Redis Sentinel
- **Message Queue**: Apache Kafka
- **Service Discovery**: Kubernetes DNS

### 운영 도구
- **Monitoring**: Prometheus + Grafana (권장)
- **Logging**: ELK Stack (권장)
- **Secret Management**: Sealed Secrets (권장)

## 📁 디렉토리 구조

```
k8s-microservices/
├── base/                           # 공통 기본 설정
│   ├── namespace.yaml
│   ├── api-gateway/               # API Gateway 리소스
│   ├── user-service/              # User Service 리소스
│   ├── payment-service/           # Payment Service 리소스
│   ├── settlement-service/        # Settlement Service 리소스
│   ├── partner-service/           # Partner Service 리소스
│   ├── accounting-service/        # Accounting Service 리소스
│   ├── redis/                     # Redis Sentinel 구조
│   ├── kafka/                     # Kafka + Zookeeper
│   └── kustomization.yaml
├── overlays/
│   ├── dev/                       # 개발 환경 오버라이드
│   │   ├── kustomization.yaml
│   │   └── patches/
│   │       ├── replica-patch.yaml
│   │       ├── resource-patch.yaml
│   │       └── service-patch.yaml
│   └── prod/                      # 운영 환경 오버라이드
│       ├── kustomization.yaml
│       ├── postgres-operator/     # PostgreSQL Operator
│       └── patches/
│           ├── replica-patch.yaml
│           ├── resource-patch.yaml
│           ├── hpa-patch.yaml
│           └── pdb-patch.yaml
└── scripts/
    ├── deploy-dev.sh              # 개발 환경 배포
    ├── deploy-prod.sh             # 운영 환경 배포
    ├── destroy-dev.sh             # 개발 환경 종료
    └── destroy-prod.sh            # 운영 환경 종료
```

## ✅ 사전 요구사항

### 필수 도구
- Kubernetes 클러스터 (v1.24+)
- kubectl (v1.24+)
- kustomize (v4.0+) 또는 kubectl에 내장된 버전

### 운영 환경 추가 요구사항
- PostgreSQL Operator (Zalando)
- Persistent Volume Provisioner
- LoadBalancer 지원 (또는 Ingress Controller)

### 권장 도구
- Helm (v3+)
- kubectx/kubens
- k9s (클러스터 관리 UI)

## 🚀 빠른 시작

### 1. 프로젝트 클론
```bash
git clone <repository-url>
cd k8s-microservices
```

### 2. 이미지 준비
```bash
# 각 서비스의 Docker 이미지를 빌드하고 레지스트리에 푸시
# overlays/dev/kustomization.yaml 또는 overlays/prod/kustomization.yaml의
# images 섹션에서 이미지 경로를 수정하세요
```

### 3. 개발 환경 배포
```bash
cd scripts
./deploy-dev.sh
```

### 4. 배포 확인
```bash
# Pod 상태 확인
kubectl get pods -n microservices -w

# 서비스 접속 테스트 (NodePort)
curl http://<node-ip>:30080/actuator/health
```

## 📖 배포 가이드

### 개발 환경 배포

```bash
# 배포
./scripts/deploy-dev.sh

# 특정 서비스만 업데이트
kubectl apply -k overlays/dev

# 로그 확인
kubectl logs -f deployment/payment-service -n microservices

# Port Forward로 로컬 접속
kubectl port-forward svc/api-gateway 8080:80 -n microservices
```

### 운영 환경 배포

```bash
# 1. PostgreSQL Operator 설치 (최초 1회)
kubectl apply -k github.com/zalando/postgres-operator/manifests

# 2. 시크릿 설정 (Vault 등 사용 권장)
# overlays/prod/kustomization.yaml의 secretGenerator 수정

# 3. 운영 환경 배포
./scripts/deploy-prod.sh

# 4. 배포 모니터링
kubectl get pods -n microservices -w
kubectl get postgresql -n microservices
```

### 환경별 차이점

| 항목 | 개발 환경 | 운영 환경 |
|------|-----------|-----------|
| 레플리카 수 | 1 | 3-5 |
| 리소스 할당 | 낮음 | 높음 |
| API Gateway Service | NodePort | LoadBalancer |
| PostgreSQL | 간단한 Deployment | Operator 클러스터 |
| HPA | 없음 | 있음 |
| PDB | 없음 | 있음 |
| 모니터링 | 선택사항 | 필수 |

## 🔧 운영 가이드

### 스케일링

```bash
# 수동 스케일링
kubectl scale deployment payment-service --replicas=10 -n microservices

# HPA 확인 (운영 환경)
kubectl get hpa -n microservices

# HPA 상세 정보
kubectl describe hpa payment-service-hpa -n microservices
```

### 롤링 업데이트

```bash
# 이미지 업데이트
kubectl set image deployment/payment-service \
  payment-service=your-registry/payment-service:v1.1.0 \
  -n microservices

# 롤아웃 상태 확인
kubectl rollout status deployment/payment-service -n microservices

# 롤백
kubectl rollout undo deployment/payment-service -n microservices
```

### 데이터베이스 관리

```bash
# PostgreSQL 클러스터 상태 확인
kubectl get postgresql postgres-cluster -n microservices

# Master Pod 접속
kubectl exec -it postgres-cluster-0 -n microservices -- psql -U postgres

# 백업 (PostgreSQL Operator 기능 사용)
kubectl annotate postgresql postgres-cluster \
  "backup"="$(date +%Y-%m-%d-%H-%M-%S)" -n microservices
```

### Redis 관리

```bash
# Sentinel 상태 확인
kubectl exec -it redis-sentinel-0 -n microservices -- redis-cli -p 26379 sentinel master mymaster

# Master 확인
kubectl exec -it redis-master-0 -n microservices -- redis-cli info replication

# Failover 테스트
kubectl exec -it redis-sentinel-0 -n microservices -- redis-cli -p 26379 sentinel failover mymaster
```

### Kafka 관리

```bash
# Kafka 클러스터 상태
kubectl exec -it kafka-0 -n microservices -- kafka-broker-api-versions --bootstrap-server localhost:9092

# 토픽 목록
kubectl exec -it kafka-0 -n microservices -- kafka-topics --list --bootstrap-server localhost:9092

# 토픽 생성
kubectl exec -it kafka-0 -n microservices -- kafka-topics \
  --create --topic payment-events \
  --partitions 3 --replication-factor 2 \
  --bootstrap-server localhost:9092
```

## 🗑️ 종료 가이드

### 개발 환경 종료
```bash
# 리소스 삭제 (데이터 보존)
./scripts/destroy-dev.sh

# 완전 삭제 (데이터 포함)
./scripts/destroy-dev.sh
# -> PVC 삭제 옵션 선택
# -> 네임스페이스 삭제 옵션 선택
```

### 운영 환경 종료
```bash
# ⚠️ 주의: 운영 환경 종료는 매우 신중하게!
./scripts/destroy-prod.sh
# -> 'DELETE PRODUCTION' 입력 필요
# -> 백업 확인 필수
```

## 🐛 문제 해결

### Pod가 시작되지 않음
```bash
# Pod 상태 확인
kubectl describe pod <pod-name> -n microservices

# 로그 확인
kubectl logs <pod-name> -n microservices

# 이벤트 확인
kubectl get events -n microservices --sort-by='.lastTimestamp'
```

### 데이터베이스 연결 실패
```bash
# PostgreSQL 서비스 확인
kubectl get svc -n microservices | grep postgres

# PostgreSQL Pod 로그
kubectl logs postgres-cluster-0 -n microservices

# 연결 테스트
kubectl run -it --rm debug --image=postgres:15 --restart=Never -n microservices -- \
  psql -h postgres-cluster-rw -U userservice -d userdb
```

### Redis 연결 실패
```bash
# Redis 서비스 확인
kubectl get svc -n microservices | grep redis

# Sentinel 상태 확인
kubectl exec -it redis-sentinel-0 -n microservices -- \
  redis-cli -p 26379 sentinel masters

# Master 상태 확인
kubectl exec -it redis-master-0 -n microservices -- redis-cli ping
```

### Kafka 연결 실패
```bash
# Kafka 서비스 확인
kubectl get svc -n microservices | grep kafka

# Zookeeper 상태
kubectl exec -it zookeeper-0 -n microservices -- \
  zkCli.sh ls /brokers/ids

# Kafka 로그
kubectl logs kafka-0 -n microservices
```

### 리소스 부족
```bash
# 노드 리소스 확인
kubectl top nodes

# Pod 리소스 사용량
kubectl top pods -n microservices

# 리소스 제한 확인
kubectl describe pod <pod-name> -n microservices | grep -A 5 "Limits"
```

## 📚 추가 문서

- [Kustomize 공식 문서](https://kustomize.io/)
- [PostgreSQL Operator 문서](https://postgres-operator.readthedocs.io/)
- [Redis Sentinel 가이드](https://redis.io/docs/management/sentinel/)
- [Kafka on Kubernetes](https://kafka.apache.org/documentation/)

## 🤝 기여

이슈나 개선 사항이 있다면 이슈를 등록하거나 Pull Request를 보내주세요.

## 📄 라이선스

[라이선스 정보 추가]

---

**주의사항**: 
- 운영 환경 배포 전 반드시 백업을 수행하세요
- 시크릿은 안전하게 관리하세요 (Vault, Sealed Secrets 등 사용 권장)
- 모니터링 시스템을 구축하세요
- 정기적인 업데이트와 보안 패치를 적용하세요
