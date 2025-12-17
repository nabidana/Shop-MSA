#!/bin/bash
###############################################################################
# 운영 환경 완전 종료 스크립트
# 사용법: ./shutdown-prod.sh
###############################################################################

set -e

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
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

log_confirm() {
    echo -e "${BLUE}[CONFIRM]${NC} $1"
}

# 변수 설정
ENVIRONMENT="prod"
NAMESPACE="payment-prod"
KUSTOMIZE_DIR="overlays/prod"

log_error "=== ⚠️  운영 환경 종료 ==="
log_error "⚠️  경고: 운영 환경의 모든 서비스가 중단됩니다!"
log_error "⚠️  이 작업은 실제 사용자에게 영향을 줍니다!"

# kubectl 체크
if ! command -v kubectl &> /dev/null; then
    log_error "kubectl이 설치되어 있지 않습니다."
    exit 1
fi

# 현재 컨텍스트 확인
CURRENT_CONTEXT=$(kubectl config current-context)
log_warn "현재 Kubernetes 컨텍스트: ${CURRENT_CONTEXT}"
log_confirm "이 컨텍스트가 올바른 운영 클러스터입니까? (yes/no)"
read -r context_confirmation
if [ "$context_confirmation" != "yes" ]; then
    log_info "종료가 취소되었습니다."
    exit 0
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

# 다중 확인 절차
echo ""
log_error "==================== 중요 알림 ===================="
log_error "운영 환경을 종료하려고 합니다!"
log_error "- 모든 서비스가 중단됩니다"
log_error "- 실제 사용자가 서비스를 이용할 수 없게 됩니다"
log_error "- 진행 중인 트랜잭션이 중단될 수 있습니다"
log_error "=================================================="
echo ""

log_confirm "정말로 운영 환경을 종료하시겠습니까? (yes/no)"
read -r first_confirmation
if [ "$first_confirmation" != "yes" ]; then
    log_info "종료가 취소되었습니다."
    exit 0
fi

# 재확인
log_confirm "다시 한번 확인합니다. 'SHUTDOWN-PROD'를 정확히 입력하세요:"
read -r second_confirmation
if [ "$second_confirmation" != "SHUTDOWN-PROD" ]; then
    log_info "입력이 일치하지 않습니다. 종료가 취소되었습니다."
    exit 0
fi

# 종료 사유 기록
log_info "종료 사유를 입력하세요 (로그에 기록됩니다):"
read -r shutdown_reason
log_info "종료 사유: ${shutdown_reason}" | tee -a /var/log/k8s-shutdown.log

# Graceful Shutdown 시작
log_info "=== Graceful Shutdown 시작 ==="

# 1. HPA 비활성화 (새로운 Pod 생성 방지)
log_info "HorizontalPodAutoscaler 비활성화..."
kubectl delete hpa --all -n ${NAMESPACE} 2>/dev/null || log_warn "HPA가 없거나 이미 삭제되었습니다."

# 2. 새로운 트래픽 차단 (LoadBalancer 서비스 삭제)
log_info "외부 트래픽 차단 (LoadBalancer 제거)..."
kubectl patch service api-gateway -n ${NAMESPACE} -p '{"spec":{"type":"ClusterIP"}}' 2>/dev/null || true

# 3. 잠시 대기 (진행 중인 요청 완료)
log_info "진행 중인 요청 완료 대기 (30초)..."
sleep 30

# 4. 애플리케이션 서비스 종료
log_info "애플리케이션 서비스 종료 중..."
kubectl scale deployment --all --replicas=0 -n ${NAMESPACE}

# 5. Deployment 완전 삭제
log_info "Deployment 삭제 대기 (60초)..."
sleep 60

# 6. Kustomize로 관리되는 모든 리소스 삭제
log_info "모든 Kubernetes 리소스 삭제 중..."
kubectl delete -k ${KUSTOMIZE_DIR} || {
    log_warn "일부 리소스 삭제 중 오류가 발생했습니다. 계속 진행합니다..."
}

# 7. StatefulSet PVC 처리
log_info ""
log_info "=== Persistent Volume Claims 처리 ==="
if kubectl get pvc -n ${NAMESPACE} &> /dev/null; then
    log_warn "중요: PVC에는 Redis와 Kafka 데이터가 포함되어 있습니다!"
    log_warn "PVC 목록:"
    kubectl get pvc -n ${NAMESPACE}
    
    echo ""
    log_confirm "PVC를 삭제하시겠습니까? (yes/no)"
    log_error "주의: 데이터가 영구적으로 삭제됩니다!"
    log_error "백업이 없다면 데이터를 복구할 수 없습니다!"
    read -r pvc_confirmation
    
    if [ "$pvc_confirmation" == "yes" ]; then
        log_confirm "'DELETE-PVC'를 입력하여 확인하세요:"
        read -r pvc_double_confirm
        
        if [ "$pvc_double_confirm" == "DELETE-PVC" ]; then
            log_warn "PVC 삭제 중... (이 작업은 취소할 수 없습니다)"
            kubectl delete pvc --all -n ${NAMESPACE}
            log_info "PVC가 삭제되었습니다."
        else
            log_info "PVC 삭제가 취소되었습니다."
            log_info "수동 삭제 명령: kubectl delete pvc --all -n ${NAMESPACE}"
        fi
    else
        log_info "PVC는 유지됩니다."
        log_info "나중에 수동으로 삭제: kubectl delete pvc --all -n ${NAMESPACE}"
    fi
fi

# 8. Pod 종료 대기
log_info ""
log_info "Pod 완전 종료 대기 중..."
timeout=300  # 5분 대기
elapsed=0
while [ $elapsed -lt $timeout ]; do
    POD_COUNT=$(kubectl get pods -n ${NAMESPACE} --no-headers 2>/dev/null | wc -l || echo "0")
    if [ "$POD_COUNT" -eq "0" ]; then
        log_info "모든 Pod가 종료되었습니다."
        break
    fi
    
    if [ $((elapsed % 30)) -eq 0 ]; then
        log_info "남은 Pod 수: ${POD_COUNT}"
    fi
    
    echo -n "."
    sleep 5
    elapsed=$((elapsed + 5))
done
echo ""

if [ $elapsed -ge $timeout ]; then
    log_warn "일부 Pod 종료에 시간이 걸리고 있습니다."
    log_info "남아있는 Pod:"
    kubectl get pods -n ${NAMESPACE} 2>/dev/null || true
    
    log_confirm "강제로 Pod를 종료하시겠습니까? (yes/no)"
    read -r force_confirmation
    if [ "$force_confirmation" == "yes" ]; then
        kubectl delete pods --all -n ${NAMESPACE} --grace-period=0 --force
    fi
fi

# 9. 네임스페이스 삭제 옵션
echo ""
log_info "=== 네임스페이스 처리 ==="
log_confirm "네임스페이스 ${NAMESPACE}도 삭제하시겠습니까? (yes/no)"
read -r namespace_confirmation

if [ "$namespace_confirmation" == "yes" ]; then
    log_info "네임스페이스 삭제 중..."
    kubectl delete namespace ${NAMESPACE}
    
    log_info "네임스페이스 삭제 완료 대기 중..."
    kubectl wait --for=delete namespace/${NAMESPACE} --timeout=300s 2>/dev/null || {
        log_warn "네임스페이스 삭제에 시간이 걸리고 있습니다."
        log_info "백그라운드에서 계속 진행됩니다."
    }
else
    log_info "네임스페이스는 유지됩니다."
fi

# 10. 최종 상태 확인
echo ""
log_info "=== 종료 완료 ==="
log_info "Environment: ${ENVIRONMENT}"
log_info "Namespace: ${NAMESPACE}"
log_info "종료 시간: $(date)"
log_info "종료 사유: ${shutdown_reason}"

# 남아있는 리소스 확인
echo ""
log_info "남아있는 리소스 확인:"
kubectl get all -n ${NAMESPACE} 2>/dev/null || log_info "네임스페이스 ${NAMESPACE}에 리소스가 없습니다."

echo ""
log_info "=== 운영 환경 종료 완료 ==="
log_warn "⚠️  서비스가 완전히 중단되었습니다."
log_info ""
log_info "다시 배포하려면:"
log_info "  ./deploy-prod.sh"
log_info ""
log_info "이전 버전으로 롤백하려면:"
log_info "  git checkout <previous-commit>"
log_info "  ./deploy-prod.sh"

exit 0
