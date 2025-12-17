#!/bin/bash

# 운영 환경 종료 스크립트
# 사용법: ./destroy-prod.sh

set -e  # 에러 발생 시 스크립트 중단

echo "========================================="
echo "  운영 환경 종료 시작"
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
echo "⚠️  ⚠️  ⚠️  경고: 운영 환경 종료 ⚠️  ⚠️  ⚠️"
echo ""
echo "🔍 현재 Kubernetes 컨텍스트:"
kubectl config current-context
echo ""

# 네임스페이스 존재 확인
if ! kubectl get namespace microservices &> /dev/null; then
    echo "ℹ️  microservices 네임스페이스가 존재하지 않습니다."
    echo "이미 삭제되었거나 배포되지 않은 상태입니다."
    exit 0
fi

# 운영 환경 종료에 대한 이중 확인
echo "⚠️  ⚠️  ⚠️  매우 중요한 경고 ⚠️  ⚠️  ⚠️"
echo ""
echo "이 작업은 운영 환경의 모든 리소스를 삭제합니다:"
echo "  - 모든 서비스가 중단됩니다"
echo "  - 데이터베이스를 포함한 모든 데이터가 삭제될 수 있습니다"
echo "  - 고객 서비스에 영향을 줄 수 있습니다"
echo ""
echo "현재 실행 중인 리소스:"
kubectl get pods -n microservices 2>/dev/null | head -20
echo ""
read -p "정말로 운영 환경을 종료하시겠습니까? 'DELETE PRODUCTION'을 입력하세요: " -r
echo ""
if [[ ! $REPLY == "DELETE PRODUCTION" ]]; then
    echo "종료를 취소했습니다."
    exit 0
fi

# 백업 확인
echo ""
echo "🔍 데이터 백업 확인"
read -p "데이터베이스와 중요 데이터의 백업을 완료하셨습니까? (yes/no): " -r
if [[ ! $REPLY == "yes" ]]; then
    echo "❌ 백업을 먼저 완료해주세요."
    exit 1
fi

# PostgreSQL 클러스터 정보 백업
echo ""
echo "📋 PostgreSQL 클러스터 정보 저장 중..."
kubectl get postgresql postgres-cluster -n microservices -o yaml > "$PROJECT_ROOT/postgres-cluster-backup.yaml" 2>/dev/null || true

# 애플리케이션 리소스 삭제 (PDB 먼저 삭제)
echo ""
echo "🗑️  PodDisruptionBudget 삭제 중..."
kubectl delete pdb --all -n microservices --ignore-not-found=true

echo ""
echo "🗑️  HorizontalPodAutoscaler 삭제 중..."
kubectl delete hpa --all -n microservices --ignore-not-found=true

# 운영 환경 리소스 삭제
echo ""
echo "🗑️  운영 환경 리소스 삭제 중..."
cd "$PROJECT_ROOT/overlays/prod"

if [[ $KUSTOMIZE_CMD == "kustomize build" ]]; then
    kustomize build . | kubectl delete -f - --ignore-not-found=true --timeout=120s
else
    kubectl delete -k . --ignore-not-found=true --timeout=120s
fi

# PostgreSQL 클러스터 삭제
echo ""
echo "🐘 PostgreSQL 클러스터 상태:"
if kubectl get postgresql -n microservices 2>/dev/null | grep -q .; then
    kubectl get postgresql -n microservices
    echo ""
    read -p "PostgreSQL 클러스터를 삭제하시겠습니까? (모든 데이터베이스 데이터가 삭제됩니다) (yes/no): " -r
    if [[ $REPLY == "yes" ]]; then
        echo "🗑️  PostgreSQL 클러스터 삭제 중..."
        kubectl delete postgresql postgres-cluster -n microservices --timeout=180s
        echo "✅ PostgreSQL 클러스터가 삭제되었습니다."
    else
        echo "ℹ️  PostgreSQL 클러스터는 유지됩니다."
    fi
else
    echo "ℹ️  삭제할 PostgreSQL 클러스터가 없습니다."
fi

# StatefulSet의 PVC 삭제 여부 확인
echo ""
echo "📊 남아있는 PersistentVolumeClaim 확인:"
if kubectl get pvc -n microservices 2> /dev/null | grep -q .; then
    kubectl get pvc -n microservices
    echo ""
    read -p "PersistentVolumeClaim도 삭제하시겠습니까? (모든 데이터가 영구 삭제됩니다) (yes/no): " -r
    if [[ $REPLY == "yes" ]]; then
        echo "🗑️  PVC 삭제 중..."
        kubectl delete pvc --all -n microservices --timeout=120s
        echo "✅ PVC가 삭제되었습니다."
    else
        echo "ℹ️  PVC는 유지됩니다."
    fi
else
    echo "ℹ️  삭제할 PVC가 없습니다."
fi

# 네임스페이스 삭제 여부 확인
echo ""
read -p "네임스페이스도 삭제하시겠습니까? (yes/no): " -r
if [[ $REPLY == "yes" ]]; then
    echo "🗑️  네임스페이스 삭제 중..."
    kubectl delete namespace microservices --timeout=180s
    echo "✅ 네임스페이스가 삭제되었습니다."
else
    echo "ℹ️  네임스페이스는 유지됩니다."
fi

echo ""
echo "========================================="
echo "  ✅ 운영 환경 종료 완료"
echo "========================================="
echo ""
echo "📝 종료 후 확인 사항:"
echo "  - 모든 리소스가 삭제되었는지 확인"
echo "  - 외부 로드밸런서가 정리되었는지 확인"
echo "  - PersistentVolume이 Reclaim 정책에 따라 처리되었는지 확인"
echo "  - 백업 파일 위치: $PROJECT_ROOT/postgres-cluster-backup.yaml"
echo ""
echo "⚠️  재배포 시 주의사항:"
echo "  1. 시크릿을 다시 생성해야 합니다"
echo "  2. PVC를 삭제했다면 데이터를 복원해야 합니다"
echo "  3. PostgreSQL Operator가 여전히 필요합니다"
echo ""
