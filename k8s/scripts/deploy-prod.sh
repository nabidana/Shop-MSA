#!/bin/bash

# 운영 환경 배포 스크립트
# 사용법: ./deploy-prod.sh

set -e  # 에러 발생 시 스크립트 중단

echo "========================================="
echo "  운영 환경 배포 시작"
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
echo "⚠️  ⚠️  ⚠️  경고: 운영 환경 배포 ⚠️  ⚠️  ⚠️"
echo ""
echo "🔍 현재 Kubernetes 컨텍스트:"
kubectl config current-context
echo ""
echo "이 작업은 운영 환경에 영향을 줍니다."
read -p "정말로 계속 진행하시겠습니까? (yes/no): " -r
echo ""
if [[ ! $REPLY == "yes" ]]; then
    echo "배포를 취소했습니다."
    exit 0
fi

# PostgreSQL Operator 설치 확인
echo ""
echo "🔍 PostgreSQL Operator 설치 확인 중..."
if ! kubectl get crd postgresqls.acid.zalan.do &> /dev/null; then
    echo "⚠️  PostgreSQL Operator가 설치되어 있지 않습니다."
    echo ""
    read -p "PostgreSQL Operator를 설치하시겠습니까? (y/N): " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "📦 PostgreSQL Operator 설치 중..."
        kubectl apply -k github.com/zalando/postgres-operator/manifests
        echo "⏳ Operator가 준비될 때까지 대기 중..."
        kubectl wait --for=condition=Available --timeout=300s \
            deployment/postgres-operator -n default
    else
        echo "❌ PostgreSQL Operator 설치 없이는 운영 환경을 배포할 수 없습니다."
        exit 1
    fi
else
    echo "✅ PostgreSQL Operator가 이미 설치되어 있습니다."
fi

# 네임스페이스 생성
echo ""
echo "📦 네임스페이스 생성 중..."
kubectl create namespace microservices --dry-run=client -o yaml | kubectl apply -f -

# 시크릿 확인
echo ""
echo "🔐 시크릿 확인 중..."
echo "⚠️  운영 환경의 시크릿은 반드시 안전하게 관리되어야 합니다."
echo "   (Vault, Sealed Secrets, External Secrets Operator 등 사용 권장)"
echo ""
read -p "시크릿이 올바르게 설정되었습니까? (yes/no): " -r
if [[ ! $REPLY == "yes" ]]; then
    echo "시크릿을 먼저 설정해주세요."
    exit 1
fi

# 운영 환경 배포
echo ""
echo "🚀 운영 환경 리소스 배포 중..."
cd "$PROJECT_ROOT/overlays/prod"

if [[ $KUSTOMIZE_CMD == "kustomize build" ]]; then
    kustomize build . | kubectl apply -f -
else
    kubectl apply -k .
fi

# 배포 상태 확인
echo ""
echo "⏳ 배포 상태 확인 중..."
echo ""

# PostgreSQL 클러스터 상태 확인
echo "🐘 PostgreSQL 클러스터 상태:"
kubectl get postgresql -n microservices
echo ""

# Pod 상태 확인
echo "📊 Pod 상태:"
kubectl get pods -n microservices -o wide

echo ""
echo "📊 Service 상태:"
kubectl get svc -n microservices

echo ""
echo "📊 StatefulSet 상태:"
kubectl get statefulset -n microservices

echo ""
echo "📊 HPA 상태:"
kubectl get hpa -n microservices

echo ""
echo "📊 PDB 상태:"
kubectl get pdb -n microservices

echo ""
echo "========================================="
echo "  ✅ 운영 환경 배포 완료"
echo "========================================="
echo ""
echo "📝 유용한 명령어:"
echo "  - Pod 로그 확인: kubectl logs -f <pod-name> -n microservices"
echo "  - Pod 상태 모니터링: kubectl get pods -n microservices -w"
echo "  - PostgreSQL 상태: kubectl get postgresql postgres-cluster -n microservices"
echo "  - HPA 모니터링: kubectl get hpa -n microservices -w"
echo ""
echo "⚠️  배포 후 다음 사항을 확인하세요:"
echo "  1. 모든 Pod가 Running 상태인지 확인"
echo "  2. PostgreSQL 클러스터가 정상 동작하는지 확인"
echo "  3. Redis Sentinel이 Master를 정상 감지하는지 확인"
echo "  4. Kafka 클러스터가 정상 동작하는지 확인"
echo "  5. 애플리케이션 Health Check 확인"
echo ""
