#!/bin/bash
###############################################################################
# 개발 환경 완전 종료 스크립트
# 사용법: ./shutdown-dev.sh
###############################################################################

set -e

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 변수 설정
ENVIRONMENT="dev"
NAMESPACE="payment-dev"
KUSTOMIZE_DIR="overlays/dev"

log_info "=== 개발 환경 종료 시작 ==="
log_info "Environment: ${ENVIRONMENT}"
log_info "Namespace: ${NAMESPACE}"

# kubectl 체크
if ! command -v kubectl &> /dev/null; then
    log_error "kubectl이 설치되어 있지 않습니다."
    exit 1
fi

# 네임스페이스 존재 확인
if ! kubectl get namespace ${NAMESPACE} &> /dev/null; then
    log_warn "네임스페이스 ${NAMESPACE}가 존재하지 않습니다."
    log_info "종료할 리소스가 없습니다."
    exit 0
fi

# 현재 리소스 상태 표시
log_info "현재 실행 중인 리소스:"
echo ""
kubectl get all -n ${NAMESPACE}

echo ""
log_warn "⚠️  위의 모든 리소스가 삭제됩니다."
log_warn "정말로 개발 환경을 종료하시겠습니까? (yes/no)"
read -r confirmation

if [ "$confirmation" != "yes" ]; then
    log_info "종료가 취소되었습니다."
    exit 0
fi

# Kustomize로 관리되는 리소스 삭제
log_info "Kustomize 관리 리소스 삭제 중..."
kubectl delete -k ${KUSTOMIZE_DIR} || {
    log_warn "일부 리소스 삭제 중 오류가 발생했습니다. 계속 진행합니다..."
}

# StatefulSet의 PVC는 자동 삭제되지 않으므로 별도 처리
log_info "Persistent Volume Claims 확인 중..."
if kubectl get pvc -n ${NAMESPACE} &> /dev/null; then
    log_warn "PVC가 남아있습니다. 삭제하시겠습니까? (yes/no)"
    log_warn "주의: 데이터가 영구적으로 삭제됩니다!"
    read -r pvc_confirmation
    
    if [ "$pvc_confirmation" == "yes" ]; then
        log_info "PVC 삭제 중..."
        kubectl delete pvc --all -n ${NAMESPACE}
    else
        log_info "PVC는 유지됩니다. 나중에 수동으로 삭제할 수 있습니다:"
        log_info "  kubectl delete pvc --all -n ${NAMESPACE}"
    fi
fi

# Pod가 완전히 종료될 때까지 대기
log_info "Pod 종료 대기 중..."
timeout=120  # 2분 대기
elapsed=0
while [ $elapsed -lt $timeout ]; do
    POD_COUNT=$(kubectl get pods -n ${NAMESPACE} --no-headers 2>/dev/null | wc -l || echo "0")
    if [ "$POD_COUNT" -eq "0" ]; then
        log_info "모든 Pod가 종료되었습니다."
        break
    fi
    echo -n "."
    sleep 5
    elapsed=$((elapsed + 5))
done
echo ""

if [ $elapsed -ge $timeout ]; then
    log_warn "일부 Pod 종료에 시간이 걸리고 있습니다."
    log_info "현재 남아있는 Pod:"
    kubectl get pods -n ${NAMESPACE} 2>/dev/null || true
fi

# 네임스페이스 삭제 옵션
echo ""
log_warn "네임스페이스 ${NAMESPACE}도 삭제하시겠습니까? (yes/no)"
log_info "네임스페이스를 삭제하면 모든 관련 리소스가 완전히 제거됩니다."
read -r namespace_confirmation

if [ "$namespace_confirmation" == "yes" ]; then
    log_info "네임스페이스 삭제 중..."
    kubectl delete namespace ${NAMESPACE}
    
    # 네임스페이스 삭제 대기
    log_info "네임스페이스 삭제 완료 대기 중..."
    kubectl wait --for=delete namespace/${NAMESPACE} --timeout=300s 2>/dev/null || {
        log_warn "네임스페이스 삭제가 완료되지 않았습니다."
        log_info "다음 명령어로 강제 삭제할 수 있습니다:"
        log_info "  kubectl delete namespace ${NAMESPACE} --grace-period=0 --force"
    }
else
    log_info "네임스페이스는 유지됩니다."
fi

echo ""
log_info "=== 개발 환경 종료 완료 ==="
log_info "남아있는 리소스 확인:"
kubectl get all -n ${NAMESPACE} 2>/dev/null || log_info "네임스페이스 ${NAMESPACE}에 리소스가 없습니다."

log_info ""
log_info "다시 배포하려면 다음 명령어를 실행하세요:"
log_info "  ./deploy-dev.sh"

exit 0
