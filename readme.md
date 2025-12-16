# Kubernetes 마이크로서비스 아키텍처

결제 시스템을 위한 Kubernetes 기반 마이크로서비스 아키텍처입니다.

## 📋 목차

- [아키텍처 개요](#아키텍처-개요)
- [디렉토리 구조](#디렉토리-구조)
- [전제 조건](#전제-조건)
- [배포 방법](#배포-방법)
- [환경별 설정](#환경별-설정)
- [모니터링](#모니터링)
- [문제 해결](#문제-해결)

## 🏗️ 아키텍처 개요

### 서비스 구성

```
├── api-gateway (Spring Cloud Gateway) - 8080
├── user-service (회원 관리) - 8081
├── payment-service (결제 처리) ⭐ - 8082
├── settlement-service (정산 처리) ⭐ - 8083
├── partner-service (파트너사 관리) - 8084
└── accounting-service (회계 처리) ⭐ - 8085
```

### 인프라 구성

- **Redis Sentinel**: 3 Sentinel + 1 Master + 2 Slaves (운영), 1 Slave (개발)
- **Kafka Cluster**: 3 Brokers (운영), 1 Broker (개발)
- **Zookeeper**: 3 Nodes (운영), 1 Node (개발)

### 이벤트 토픽

- `payment-events`: 결제 이벤트
- `settlement-events`: 정산 이벤트
- `accounting-events`: 회계 이벤트
- `partner-events`: 파트너 이벤트

## 📁 디렉토리 구조

```
k8s/
├── base/                          # Base 설정 (공통)
│   ├── api-gateway/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── kustomization.yaml
│   ├── user-service/
│   ├── payment-service/
│   ├── settlement-service/
│   ├── partner-service/
│   ├── accounting-service/
│   ├── redis/                     # Redis Sentinel 구성
│   │   ├── configmap.yaml
│   │   ├── master-statefulset.yaml
│   │   ├── slave-statefulset.yaml
│   │   ├── sentinel-statefulset.yaml
│   │   ├── service.yaml
│   │   └── kustomization.yaml
│   ├── kafka/                     # Kafka 클러스터 구성
│   │   ├── zookeeper-statefulset.yaml
│   │   ├── statefulset.yaml
│   │   ├── service.yaml
│   │   └── kustomization.yaml
│   └── kustomization.yaml
│
├── overlays/                      # 환경별 설정
│   ├── dev/                       # 개발 환경
│   │   ├── namespace.yaml
│   │   ├── secrets.yaml
│   │   ├── replica-patch.yaml
│   │   ├── redis-patch.yaml
│   │   ├── kafka-patch.yaml
│   │   └── kustomization.yaml
│   └── prod/                      # 운영 환경
│       ├── namespace.yaml
│       ├── secrets.yaml
│       ├── replica-patch.yaml
│       ├── redis-patch.yaml
│       ├── kafka-patch.yaml
│       ├── hpa.yaml               # Auto-scaling
│       └── kustomization.yaml
│
└── scripts/                       # 배포/종료 스크립트
    ├── deploy-dev.sh
    ├── deploy-prod.sh
    ├── shutdown-dev.sh
    └── shutdown-prod.sh
```

## ✅ 전제 조건

### 필수 도구

```bash
# kubectl 설치 확인
kubectl version --client

# Kustomize는 kubectl에 내장되어 있음
kubectl kustomize --help
```

### Kubernetes 클러스터

- Kubernetes 1.24 이상
- 충분한 노드 리소스:
  - 개발: 최소 4 vCPU, 8GB RAM
  - 운영: 최소 16 vCPU, 32GB RAM

### 컨테이너 이미지

다음 이미지를 컨테이너 레지스트리에 빌드 및 푸시해야 합니다:

```bash
your-registry/api-gateway:tag
your-registry/user-service:tag
your-registry/payment-service:tag
your-registry/settlement-service:tag
your-registry/partner-service:tag
your-registry/accounting-service:tag
```

## 🚀 배포 방법

### 개발 환경 배포

```bash
cd k8s-microservices

# 1. 이미지 레지스트리 설정 (overlays/dev/kustomization.yaml 수정)
# 2. Secret 값 수정 (overlays/dev/secrets.yaml)

# 3. 배포 실행
./scripts/deploy-dev.sh

# 4. 상태 확인
kubectl get all -n payment-dev
kubectl logs -n payment-dev <pod-name>
```

### 운영 환경 배포

```bash
cd k8s-microservices

# 1. 운영 클러스터 컨텍스트 설정
kubectl config use-context prod-cluster

# 2. 이미지 레지스트리 및 태그 설정 (overlays/prod/kustomization.yaml)
# 3. Secret 값 수정 (overlays/prod/secrets.yaml)
# 주의: 실제 운영에서는 Secret을 Git에 커밋하지 말 것!

# 4. 배포 실행 (확인 절차 포함)
./scripts/deploy-prod.sh

# 5. 모니터링
kubectl get all -n payment-prod
kubectl top pods -n payment-prod
```

### 수동 배포 (Kustomize 직접 사용)

```bash
# 개발 환경
kubectl apply -k overlays/dev

# 운영 환경
kubectl apply -k overlays/prod

# Dry-run (실제 적용하지 않고 확인)
kubectl apply --dry-run=client -k overlays/dev

# 변경 사항 미리보기
kubectl diff -k overlays/prod
```

## 🛑 종료 방법

### 개발 환경 종료

```bash
./scripts/shutdown-dev.sh

# 또는 수동 종료
kubectl delete -k overlays/dev
kubectl delete namespace payment-dev
```

### 운영 환경 종료

```bash
# 주의: 운영 환경 종료는 신중하게!
./scripts/shutdown-prod.sh

# 스크립트는 다음을 수행합니다:
# 1. 다중 확인 절차
# 2. HPA 비활성화
# 3. 외부 트래픽 차단
# 4. Graceful shutdown (30초 대기)
# 5. 리소스 삭제
# 6. PVC 처리 (선택)
```

## ⚙️ 환경별 설정

### 개발 환경 (Dev)

- **네임스페이스**: `payment-dev`
- **레플리카 수**: 각 서비스 1개
- **Redis**: Master 1, Slave 1, Sentinel 3
- **Kafka**: Broker 1, Zookeeper 1
- **리소스**: 최소 설정
- **로그 레벨**: DEBUG

### 운영 환경 (Prod)

- **네임스페이스**: `payment-prod`
- **레플리카 수**:
  - api-gateway: 3개
  - payment-service: 5개 (중요)
  - 기타 서비스: 3개
- **Redis**: Master 1, Slave 2, Sentinel 3
- **Kafka**: Broker 3, Zookeeper 3
- **Auto-scaling**: HPA 설정
- **리소스**: 충분한 할당
- **로그 레벨**: INFO

## 📊 모니터링

### Pod 상태 확인

```bash
# 모든 리소스 확인
kubectl get all -n payment-dev

# Pod 상태 확인
kubectl get pods -n payment-dev -o wide

# 특정 Pod 로그 확인
kubectl logs -n payment-dev <pod-name> -f

# 이전 실행 로그 확인
kubectl logs -n payment-dev <pod-name> --previous
```

### 리소스 사용량

```bash
# CPU/Memory 사용량
kubectl top pods -n payment-prod
kubectl top nodes

# HPA 상태 (운영)
kubectl get hpa -n payment-prod
```

### Health Check

```bash
# API Gateway 헬스 체크
kubectl port-forward -n payment-dev svc/api-gateway 8080:80
curl http://localhost:8080/actuator/health

# 각 서비스별 헬스 체크
kubectl exec -n payment-dev <pod-name> -- curl http://localhost:8081/actuator/health
```

### Redis Sentinel 상태

```bash
# Sentinel 상태 확인
kubectl exec -n payment-dev redis-sentinel-0 -- redis-cli -p 26379 SENTINEL masters

# Master 확인
kubectl exec -n payment-dev redis-sentinel-0 -- redis-cli -p 26379 SENTINEL get-master-addr-by-name mymaster

# Slave 목록
kubectl exec -n payment-dev redis-sentinel-0 -- redis-cli -p 26379 SENTINEL slaves mymaster
```

### Kafka 상태

```bash
# Kafka 브로커 목록
kubectl exec -n payment-dev kafka-0 -- kafka-broker-api-versions --bootstrap-server localhost:9092

# 토픽 목록
kubectl exec -n payment-dev kafka-0 -- kafka-topics --bootstrap-server localhost:9092 --list

# 토픽 상세 정보
kubectl exec -n payment-dev kafka-0 -- kafka-topics --bootstrap-server localhost:9092 --describe --topic payment-events
```

## 🔧 문제 해결

### Pod가 시작되지 않을 때

```bash
# Pod 상태 확인
kubectl describe pod <pod-name> -n payment-dev

# 이벤트 확인
kubectl get events -n payment-dev --sort-by='.lastTimestamp'

# 이미지 Pull 오류 확인
kubectl get pods -n payment-dev -o jsonpath='{.items[*].status.containerStatuses[*].state.waiting.reason}'
```

### Redis Sentinel Failover 테스트

```bash
# Master Pod 삭제하여 Failover 테스트
kubectl delete pod redis-master-0 -n payment-dev

# Failover 확인
kubectl exec -n payment-dev redis-sentinel-0 -- redis-cli -p 26379 SENTINEL masters
```

### 롤백

```bash
# Deployment 롤백
kubectl rollout undo deployment/payment-service -n payment-prod

# 특정 리비전으로 롤백
kubectl rollout undo deployment/payment-service -n payment-prod --to-revision=2

# 롤아웃 히스토리 확인
kubectl rollout history deployment/payment-service -n payment-prod
```

### Secret 관리

```bash
# Secret 생성 (실제 환경에서는 Sealed Secrets 사용 권장)
kubectl create secret generic payment-service-secret \
  --from-literal=db.username=user \
  --from-literal=db.password=pass \
  -n payment-dev

# Secret 확인 (Base64 디코딩)
kubectl get secret payment-service-secret -n payment-dev -o jsonpath='{.data.db\.password}' | base64 -d
```

## 📚 추가 자료

- [Kustomize 공식 문서](https://kustomize.io/)
- [Kubernetes 공식 문서](https://kubernetes.io/docs/)
- [Redis Sentinel 문서](https://redis.io/topics/sentinel)
- [Apache Kafka 문서](https://kafka.apache.org/documentation/)

## 🔒 보안 고려사항

1. **Secret 관리**:
   - Git에 Secret을 커밋하지 마세요
   - Sealed Secrets, AWS Secrets Manager, Vault 사용 권장

2. **네트워크 정책**:
   - NetworkPolicy를 사용하여 Pod 간 통신 제한

3. **RBAC**:
   - 최소 권한 원칙 적용

4. **이미지 보안**:
   - 신뢰할 수 있는 이미지만 사용
   - 정기적인 취약점 스캔

## 📞 지원

문제가 발생하면 다음을 확인하세요:

1. Pod 로그: `kubectl logs -n <namespace> <pod-name>`
2. 이벤트: `kubectl get events -n <namespace>`
3. 리소스 상태: `kubectl describe <resource> <name> -n <namespace>`
