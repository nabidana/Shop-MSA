#!/bin/bash

# 테스트 환경 인프라 종료 스크립트
# 사용법: ./stop-test-infra.sh

set -e  # 에러 발생 시 스크립트 중단

echo "========================================="
echo "  테스트 환경 인프라 종료"
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
    KUSTOMIZE_CMD="kubectl delete -k"
else
    echo "✅ kustomize 발견"
    KUSTOMIZE_CMD="kustomize build"
fi

# 현재 컨텍스트 확인
echo ""
echo "🔍 현재 Kubernetes 컨텍스트:"
kubectl config current-context
echo ""

# 네임스페이스 존재 확인
if ! kubectl get namespace shop-msa &> /dev/null; then
    echo "ℹ️  shop-msa 네임스페이스가 존재하지 않습니다."
    echo "이미 삭제되었거나 배포되지 않은 상태입니다."
    exit 0
fi

echo "⚠️  경고: 이 작업은 테스트 인프라의 모든 리소스를 삭제합니다."
echo "   - PostgreSQL, Redis, Kafka가 중단됩니다"
echo "   - 데이터가 삭제될 수 있습니다"
echo ""
read -p "계속 진행하시겠습니까? (y/N): " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "종료를 취소했습니다."
    exit 0
fi

# 테스트 환경 인프라 삭제
echo ""
echo "🗑️  테스트 환경 인프라 삭제 중..."
cd "$PROJECT_ROOT/overlays/test"

if [[ $KUSTOMIZE_CMD == "kustomize build" ]]; then
    kustomize build . | kubectl delete -f - --ignore-not-found=true
else
    kubectl delete -k . --ignore-not-found=true
fi

# StatefulSet의 PVC 삭제 여부 확인
echo ""
echo "📊 남아있는 PersistentVolumeClaim 확인:"
if kubectl get pvc -n shop-msa 2> /dev/null | grep -q .; then
    kubectl get pvc -n shop-msa
    echo ""
    read -p "PersistentVolumeClaim도 삭제하시겠습니까? (데이터가 영구 삭제됩니다) (y/N): " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "🗑️  PVC 삭제 중..."
        kubectl delete pvc --all -n shop-msa
        echo "✅ PVC가 삭제되었습니다."
    else
        echo "ℹ️  PVC는 유지됩니다. 다음 배포 시 기존 데이터를 재사용합니다."
    fi
else
    echo "ℹ️  삭제할 PVC가 없습니다."
fi

# 네임스페이스 삭제 여부 확인
echo ""
read -p "네임스페이스도 삭제하시겠습니까? (다른 리소스가 있다면 함께 삭제됩니다) (y/N): " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🗑️  네임스페이스 삭제 중..."
    kubectl delete namespace shop-msa --timeout=60s
    echo "✅ 네임스페이스가 삭제되었습니다."
else
    echo "ℹ️  네임스페이스는 유지됩니다."
fi

echo ""
echo "========================================="
echo "  ✅ 테스트 환경 인프라 종료 완료"
echo "========================================="
echo ""
echo "📝 참고:"
echo "  - PVC를 삭제하지 않았다면 데이터는 보존됩니다"
echo "  - 재배포 시 ./start-test-infra.sh 실행"
echo "  - 완전히 초기화하려면 PVC도 삭제해야 합니다"
echo ""
