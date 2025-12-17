# PostgreSQL 설정 가이드

이 프로젝트는 환경별로 다른 PostgreSQL 설정을 사용합니다.

## 📋 환경별 PostgreSQL 구성

### 개발 환경 (Dev)
- **타입**: 단순 StatefulSet
- **레플리카**: 1개
- **스토리지**: 5Gi
- **위치**: `base/postgresql/`
- **자동 초기화**: 5개의 데이터베이스 자동 생성
  - userdb (userservice)
  - paymentdb (paymentservice)
  - settlementdb (settlementservice)
  - partnerdb (partnerservice)
  - accountingdb (accountingservice)

### 운영 환경 (Prod)
- **타입**: PostgreSQL Operator 클러스터
- **레플리카**: 3개 (Master 1 + Replica 2)
- **스토리지**: 50Gi
- **고가용성**: 자동 Failover 지원
- **추가 기능**: 
  - 자동 백업
  - Connection Pooler (PgBouncer)
  - 모니터링 지원

## 🚀 빠른 시작

### 개발 환경

```bash
# 1. 배포
./scripts/deploy-dev.sh

# 2. PostgreSQL 접속 확인
kubectl exec -it postgresql-0 -n microservices -- psql -U postgres

# 3. 데이터베이스 확인
\l

# 4. 특정 데이터베이스 접속
\c userdb

# 5. 유저 확인
\du
```

### 운영 환경

```bash
# 1. PostgreSQL Operator 설치 (최초 1회)
kubectl apply -k github.com/zalando/postgres-operator/manifests

# 2. Operator 준비 대기
kubectl wait --for=condition=Available --timeout=300s \
    deployment/postgres-operator -n default

# 3. 운영 환경 배포
./scripts/deploy-prod.sh

# 4. PostgreSQL 클러스터 확인
kubectl get postgresql postgres-cluster -n microservices

# 5. Master Pod 접속
kubectl exec -it postgres-cluster-0 -n microservices -- psql -U postgres
```

## 🔧 연결 정보

### 개발 환경
```yaml
Host: postgresql.microservices.svc.cluster.local
Port: 5432

# 각 서비스별 계정
userdb:
  username: userservice
  password: dev_user_password (Secret에서 관리)

paymentdb:
  username: paymentservice
  password: dev_payment_password

settlementdb:
  username: settlementservice
  password: dev_settlement_password

partnerdb:
  username: partnerservice
  password: dev_partner_password

accountingdb:
  username: accountingservice
  password: dev_accounting_password
```

### 운영 환경
```yaml
# Read-Write (Master)
Host: postgres-cluster-rw.microservices.svc.cluster.local
Port: 5432

# Read-Only (Replica)
Host: postgres-cluster-ro.microservices.svc.cluster.local
Port: 5432

# Connection Pooler
Host: postgres-cluster-pooler.microservices.svc.cluster.local
Port: 5432

# 계정 정보는 Operator가 자동 생성한 Secret 사용
Secret Name Pattern: {username}.postgres-cluster.credentials.postgresql.acid.zalan.do
```

## 📊 데이터베이스 스키마

각 서비스는 독립적인 데이터베이스를 사용합니다:

```
PostgreSQL Cluster
├── userdb (User Service)
│   └── 사용자 정보, 인증, 권한
├── paymentdb (Payment Service)
│   └── 결제 내역, 결제 수단
├── settlementdb (Settlement Service)
│   └── 정산 정보, 정산 내역
├── partnerdb (Partner Service)
│   └── 파트너사 정보, 계약
└── accountingdb (Accounting Service)
    └── 회계 전표, 장부
```

## 🔐 시크릿 관리

### 개발 환경
시크릿은 `overlays/dev/kustomization.yaml`의 `secretGenerator`에서 관리합니다.

```yaml
secretGenerator:
  - name: postgres-secret
    behavior: merge
    literals:
      - postgres-password=dev_postgres_password
      - userservice-password=dev_user_password
      # ... 기타 서비스
```

### 운영 환경
**⚠️ 중요**: 운영 환경에서는 반드시 외부 시크릿 관리 도구를 사용하세요!

권장 도구:
- HashiCorp Vault
- Sealed Secrets
- External Secrets Operator
- AWS Secrets Manager / Azure Key Vault

## 🛠️ 유지보수

### 백업

#### 개발 환경
```bash
# Pod에서 직접 백업
kubectl exec postgresql-0 -n microservices -- \
  pg_dumpall -U postgres > backup.sql

# 복원
kubectl exec -i postgresql-0 -n microservices -- \
  psql -U postgres < backup.sql
```

#### 운영 환경
```bash
# PostgreSQL Operator의 백업 기능 사용
kubectl annotate postgresql postgres-cluster \
  "backup"="$(date +%Y-%m-%d-%H-%M-%S)" -n microservices

# 백업 목록 확인
kubectl get backups -n microservices
```

### 스케일링

#### 개발 환경
```bash
# 개발 환경은 단일 인스턴스 권장 (스케일링 불필요)
```

#### 운영 환경
```bash
# Replica 수 조정 (Operator CRD 수정)
kubectl patch postgresql postgres-cluster -n microservices \
  --type merge -p '{"spec":{"numberOfInstances":5}}'

# 상태 확인
kubectl get postgresql postgres-cluster -n microservices -w
```

### 모니터링

#### 개발 환경
```bash
# 로그 확인
kubectl logs -f postgresql-0 -n microservices

# 연결 수 확인
kubectl exec postgresql-0 -n microservices -- \
  psql -U postgres -c "SELECT count(*) FROM pg_stat_activity;"
```

#### 운영 환경
```bash
# Prometheus 메트릭 확인 (postgres-exporter 포함)
kubectl port-forward svc/postgres-cluster-metrics 9187:9187 -n microservices

# Grafana 대시보드 사용 권장
```

## 🐛 문제 해결

### 연결 실패
```bash
# 1. Service 확인
kubectl get svc -n microservices | grep postgres

# 2. Pod 상태 확인
kubectl get pods -n microservices | grep postgres

# 3. 로그 확인
kubectl logs postgresql-0 -n microservices

# 4. 연결 테스트
kubectl run -it --rm debug --image=postgres:15 --restart=Never -n microservices -- \
  psql -h postgresql -U postgres
```

### 초기화 실패 (개발환경)
```bash
# ConfigMap 확인
kubectl get configmap postgres-config -n microservices -o yaml

# Pod 재시작
kubectl delete pod postgresql-0 -n microservices
```

### Operator 문제 (운영환경)
```bash
# Operator 로그 확인
kubectl logs -l name=postgres-operator -n default

# PostgreSQL 클러스터 상태 확인
kubectl describe postgresql postgres-cluster -n microservices

# 이벤트 확인
kubectl get events -n microservices --sort-by='.lastTimestamp' | grep postgres
```

## 📚 추가 자료

- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/)
- [Zalando PostgreSQL Operator](https://postgres-operator.readthedocs.io/)
- [PostgreSQL High Availability](https://www.postgresql.org/docs/current/high-availability.html)

## ⚠️ 주의사항

1. **개발 환경 데이터는 영구적이지 않습니다**
   - PVC를 삭제하면 모든 데이터가 손실됩니다
   - 테스트용으로만 사용하세요

2. **운영 환경 백업은 필수입니다**
   - 정기적인 백업 스케줄을 설정하세요
   - 백업 복원 테스트를 주기적으로 수행하세요

3. **시크릿 관리에 주의하세요**
   - Git에 시크릿을 커밋하지 마세요
   - 운영 환경은 반드시 외부 시크릿 관리 도구를 사용하세요

4. **리소스 모니터링**
   - CPU, 메모리, 스토리지 사용량을 모니터링하세요
   - 적절한 리소스 제한을 설정하세요
