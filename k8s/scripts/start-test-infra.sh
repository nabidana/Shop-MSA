#!/bin/bash

# 테스트 환경 인프라 실행 스크립트 (PostgreSQL, Redis, Kafka)
# 사용법: ./start-test-infra.sh

set -e  # 에러 발생 시 스크립트 중단

echo "========================================="
echo "  테스트 환경 인프라 실행"
echo "  - PostgreSQL"
echo "  - Redis Sentinel"
echo "  - Kafka + Zookeeper"
echo "========================================="

# 현재 디렉토리 확인
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

echo ""
echo "📁 프로젝트 루트: $PROJECT_ROOT"
echo ""

# kubectl 설치 확인
if ! command -v kubectl &> /dev/null; then
    echo "❌ kubectl이 설치되어 있지 않습니다."
    exit 1
fi

# kustomize 설치 확인
if ! command -v kustomize &> /dev/null; then
    echo "⚠️  kustomize가 설치되어 있지 않습니다. kubectl에 내장된 버전을 사용합니다."
    KUSTOMIZE_CMD="kubectl apply -k"
else
    echo "✅ kustomize 발견"
    KUSTOMIZE_CMD="kustomize build"
fi

# 현재 컨텍스트 확인
echo ""
echo "🔍 현재 Kubernetes 컨텍스트:"
kubectl config current-context
echo ""
read -p "계속 진행하시겠습니까? (y/N): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "실행을 취소했습니다."
    exit 0
fi

# 네임스페이스 생성
echo ""
echo "📦 네임스페이스 생성 중..."
kubectl create namespace microservices --dry-run=client -o yaml | kubectl apply -f -

# 테스트 환경 인프라 배포
echo ""
echo "🚀 테스트 환경 인프라 배포 중..."
cd "$PROJECT_ROOT/overlays/test"

if [[ $KUSTOMIZE_CMD == "kustomize build" ]]; then
    kustomize build . | kubectl apply -f -
else
    kubectl apply -k .
fi

# 배포 상태 확인
echo ""
echo "⏳ 배포 상태 확인 중..."
echo ""

# Pod 상태 확인
echo "📊 Pod 상태:"
kubectl get pods -n microservices -l component=postgresql -o wide
kubectl get pods -n microservices -l component=redis-sentinel -o wide
kubectl get pods -n microservices -l component=kafka-cluster -o wide

echo ""
echo "📊 Service 상태:"
kubectl get svc -n microservices

echo ""
echo "📊 PersistentVolumeClaim 상태:"
kubectl get pvc -n microservices

echo ""
echo "========================================="
echo "  ✅ 테스트 환경 인프라 배포 완료"
echo "========================================="
echo ""
echo "📝 연결 정보:"
echo ""
echo "🐘 PostgreSQL:"
echo "  Host: postgresql.microservices.svc.cluster.local"
echo "  Port: 5432"
echo "  Databases: userdb, paymentdb, settlementdb, partnerdb, accountingdb"
echo "  Username: {service}service (예: userservice)"
echo "  Password: test_{service}_password"
echo ""
echo "  # Pod 내부에서 접속:"
echo "  kubectl exec -it postgresql-0 -n microservices -- psql -U postgres"
echo ""
echo "  # 로컬에서 Port Forward:"
echo "  kubectl port-forward svc/postgresql 5432:5432 -n microservices"
echo "  psql -h localhost -U postgres"
echo ""
echo "🔴 Redis:"
echo "  Master: redis-master.microservices.svc.cluster.local:6379"
echo "  Sentinel: redis-sentinel.microservices.svc.cluster.local:26379"
echo ""
echo "  # Redis CLI 접속:"
echo "  kubectl exec -it redis-master-0 -n microservices -- redis-cli"
echo ""
echo "  # 로컬에서 Port Forward:"
echo "  kubectl port-forward svc/redis-master 6379:6379 -n microservices"
echo ""
echo "📨 Kafka:"
echo "  Bootstrap Servers: kafka.microservices.svc.cluster.local:9092"
echo ""
echo "  # Kafka 토픽 확인:"
echo "  kubectl exec -it kafka-0 -n microservices -- kafka-topics --list --bootstrap-server localhost:9092"
echo ""
echo "  # 로컬에서 Port Forward:"
echo "  kubectl port-forward svc/kafka 9092:9092 -n microservices"
echo ""
echo "========================================="
echo "💡 유용한 명령어:"
echo "  - Pod 로그 확인: kubectl logs -f <pod-name> -n microservices"
echo "  - Pod 상태 모니터링: kubectl get pods -n microservices -w"
echo "  - 인프라 종료: ./stop-test-infra.sh"
echo "========================================="
echo ""
echo "⏰ 모든 Pod가 Ready 상태가 될 때까지 기다려주세요."
echo "   상태 확인: kubectl get pods -n microservices -w"
echo ""
