#!/bin/bash

# 개발 환경 배포 스크립트
# 사용법: ./deploy-dev.sh

set -e  # 에러 발생 시 스크립트 중단

echo "========================================="
echo "  개발 환경 배포 시작"
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
    echo "배포를 취소했습니다."
    exit 0
fi

# 네임스페이스 생성
echo ""
echo "📦 네임스페이스 생성 중..."
kubectl create namespace microservices --dry-run=client -o yaml | kubectl apply -f -

# 개발 환경 배포
echo ""
echo "🚀 개발 환경 리소스 배포 중..."
cd "$PROJECT_ROOT/overlays/dev"

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
kubectl get pods -n microservices -o wide

echo ""
echo "📊 Service 상태:"
kubectl get svc -n microservices

echo ""
echo "📊 StatefulSet 상태:"
kubectl get statefulset -n microservices

echo ""
echo "========================================="
echo "  ✅ 개발 환경 배포 완료"
echo "========================================="
echo ""
echo "📝 유용한 명령어:"
echo "  - Pod 로그 확인: kubectl logs -f <pod-name> -n microservices"
echo "  - Pod 상태 모니터링: kubectl get pods -n microservices -w"
echo "  - 서비스 접속 확인: kubectl port-forward svc/api-gateway 8080:80 -n microservices"
echo "  - API Gateway URL (NodePort): http://<node-ip>:30080"
echo ""
