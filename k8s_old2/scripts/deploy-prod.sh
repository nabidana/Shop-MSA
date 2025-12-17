#!/bin/bash
###############################################################################
# 운영 환경 배포 스크립트
# 사용법: ./deploy-prod.sh
###############################################################################

set -e  # 에러 발생 시 스크립트 중단

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 로그 함수
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

log_info "=== 운영 환경 배포 시작 ==="
log_warn "⚠️  운영 환경 배포는 신중하게 진행해야 합니다!"

# 운영 환경 배포 확인
log_confirm "정말로 운영 환경에 배포하시겠습니까? (yes/no)"
read -r confirmation
if [ "$confirmation" != "yes" ]; then
    log_info "배포가 취소되었습니다."
    exit 0
fi

log_info "Environment: ${ENVIRONMENT}"
log_info "Namespace: ${NAMESPACE}"

# kubectl 체크
if ! command -v kubectl &> /dev/null; then
    log_error "kubectl이 설치되어 있지 않습니다."
    exit 1
fi

# 현재 컨텍스트 확인
CURRENT_CONTEXT=$(kubectl config current-context)
log_info "현재 Kubernetes 컨텍스트: ${CURRENT_CONTEXT}"
log_confirm "이 컨텍스트가 올바른 운영 클러스터입니까? (yes/no)"
read -r context_confirmation
if [ "$context_confirmation" != "yes" ]; then
    log_info "배포가 취소되었습니다. 올바른 컨텍스트로 전환 후 다시 시도하세요."
    log_info "컨텍스트 확인: kubectl config get-contexts"
    log_info "컨텍스트 변경: kubectl config use-context <context-name>"
    exit 0
fi

# Kustomize 체크
log_info "Kustomize 버전 확인..."
kubectl kustomize --help &> /dev/null || {
    log_error "kubectl kustomize를 사용할 수 없습니다."
    exit 1
}

# 네임스페이스 생성 확인
log_info "네임스페이스 확인 및 생성..."
kubectl get namespace ${NAMESPACE} &> /dev/null || {
    log_warn "네임스페이스 ${NAMESPACE}가 존재하지 않습니다. 생성 중..."
    kubectl create namespace ${NAMESPACE}
}

# Dry-run으로 먼저 확인
log_info "배포 설정 검증 중 (dry-run)..."
kubectl apply --dry-run=client -k ${KUSTOMIZE_DIR} > /dev/null 2>&1 || {
    log_error "Kustomize 설정에 오류가 있습니다."
    exit 1
}

# 배포될 리소스 미리보기
log_info "배포될 리소스 미리보기:"
kubectl diff -k ${KUSTOMIZE_DIR} || true

echo ""
log_confirm "위 변경사항을 적용하시겠습니까? (yes/no)"
read -r apply_confirmation
if [ "$apply_confirmation" != "yes" ]; then
    log_info "배포가 취소되었습니다."
    exit 0
fi

# 실제 배포
log_info "리소스 배포 중..."
kubectl apply -k ${KUSTOMIZE_DIR}

# 배포 상태 확인
log_info "배포 완료. 리소스 상태 확인 중..."
echo ""

log_info "=== Deployments 상태 ==="
kubectl get deployments -n ${NAMESPACE} -o wide

echo ""
log_info "=== StatefulSets 상태 ==="
kubectl get statefulsets -n ${NAMESPACE} -o wide

echo ""
log_info "=== HorizontalPodAutoscalers 상태 ==="
kubectl get hpa -n ${NAMESPACE}

echo ""
log_info "=== Services 상태 ==="
kubectl get services -n ${NAMESPACE} -o wide

echo ""
log_info "=== Pods 상태 ==="
kubectl get pods -n ${NAMESPACE} -o wide

# Pod 준비 대기
log_info ""
log_info "중요 서비스 Pod가 Ready 상태가 될 때까지 대기 중..."

# 각 Deployment의 rollout 상태 확인
for deploy in api-gateway user-service payment-service settlement-service partner-service accounting-service; do
    log_info "Waiting for ${deploy}..."
    kubectl rollout status deployment/${deploy} -n ${NAMESPACE} --timeout=10m || {
        log_error "${deploy} 배포가 실패했습니다!"
        log_info "롤백을 고려하세요: kubectl rollout undo deployment/${deploy} -n ${NAMESPACE}"
        exit 1
    }
done

# Redis 및 Kafka StatefulSet 확인
for sts in redis-master redis-slave redis-sentinel kafka zookeeper; do
    log_info "Waiting for ${sts}..."
    kubectl rollout status statefulset/${sts} -n ${NAMESPACE} --timeout=10m || {
        log_error "${sts} StatefulSet 배포가 실패했습니다!"
        exit 1
    }
done

# 헬스체크
log_info ""
log_info "=== 서비스 헬스 체크 ==="
sleep 10  # Pod가 완전히 준비될 때까지 대기

for deploy in api-gateway user-service payment-service settlement-service partner-service accounting-service; do
    POD=$(kubectl get pod -n ${NAMESPACE} -l app=${deploy} -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
    if [ -n "$POD" ]; then
        log_info "Checking ${deploy}..."
        kubectl exec -n ${NAMESPACE} ${POD} -- curl -s http://localhost:8080/actuator/health > /dev/null 2>&1 && {
            log_info "✓ ${deploy} is healthy"
        } || {
            log_warn "✗ ${deploy} health check failed (may need more time)"
        }
    fi
done

echo ""
log_info "=== 운영 환경 배포 완료 ==="
log_info "네임스페이스: ${NAMESPACE}"
log_info ""
log_info "다음 명령어로 상태를 확인할 수 있습니다:"
log_info "  kubectl get all -n ${NAMESPACE}"
log_info "  kubectl top pods -n ${NAMESPACE}  # 리소스 사용량"
log_info "  kubectl logs -n ${NAMESPACE} <pod-name>"
log_info ""
log_info "모니터링 대시보드 확인:"
log_info "  kubectl port-forward -n ${NAMESPACE} svc/api-gateway 8080:80"

# API Gateway 엔드포인트 출력
echo ""
log_info "=== API Gateway 엔드포인트 ==="
kubectl get service api-gateway -n ${NAMESPACE}

log_info ""
log_info "⚠️  배포 후 모니터링을 지속적으로 확인하세요!"
log_info "문제 발생 시 롤백 명령: ./shutdown-prod.sh && git checkout <previous-commit> && ./deploy-prod.sh"

exit 0
