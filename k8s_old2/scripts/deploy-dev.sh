#!/bin/bash
###############################################################################
# 개발 환경 배포 스크립트
# 사용법: ./deploy-dev.sh
###############################################################################

set -e  # 에러 발생 시 스크립트 중단

# 색상 정의
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
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

# 변수 설정
ENVIRONMENT="dev"
NAMESPACE="payment-dev"
KUSTOMIZE_DIR="overlays/dev"

log_info "=== 개발 환경 배포 시작 ==="
log_info "Environment: ${ENVIRONMENT}"
log_info "Namespace: ${NAMESPACE}"

# kubectl 체크
if ! command -v kubectl &> /dev/null; then
    log_error "kubectl이 설치되어 있지 않습니다."
    exit 1
fi

# Kustomize 체크 (kubectl에 내장)
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

# Dry-run으로 먼저 확인 (선택사항)
log_info "배포 설정 검증 중 (dry-run)..."
kubectl apply --dry-run=client -k ${KUSTOMIZE_DIR} > /dev/null 2>&1 || {
    log_error "Kustomize 설정에 오류가 있습니다."
    exit 1
}

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
log_info "=== Services 상태 ==="
kubectl get services -n ${NAMESPACE} -o wide

echo ""
log_info "=== Pods 상태 ==="
kubectl get pods -n ${NAMESPACE} -o wide

# Pod 준비 대기 (선택사항)
log_info ""
log_info "Pod가 Ready 상태가 될 때까지 대기 중..."
log_warn "이 작업은 시간이 걸릴 수 있습니다. Ctrl+C로 중단할 수 있습니다."

# 각 Deployment의 rollout 상태 확인
for deploy in api-gateway user-service payment-service settlement-service partner-service accounting-service; do
    log_info "Waiting for ${deploy}..."
    kubectl rollout status deployment/${deploy} -n ${NAMESPACE} --timeout=5m || {
        log_warn "${deploy} 배포가 시간 내에 완료되지 않았습니다."
    }
done

# Redis 및 Kafka StatefulSet 확인
for sts in redis-master redis-slave redis-sentinel kafka zookeeper; do
    log_info "Waiting for ${sts}..."
    kubectl rollout status statefulset/${sts} -n ${NAMESPACE} --timeout=5m || {
        log_warn "${sts} StatefulSet이 시간 내에 완료되지 않았습니다."
    }
done

echo ""
log_info "=== 개발 환경 배포 완료 ==="
log_info "네임스페이스: ${NAMESPACE}"
log_info ""
log_info "다음 명령어로 상태를 확인할 수 있습니다:"
log_info "  kubectl get all -n ${NAMESPACE}"
log_info "  kubectl logs -n ${NAMESPACE} <pod-name>"
log_info ""
log_info "API Gateway 엔드포인트 확인:"
kubectl get service api-gateway -n ${NAMESPACE}

exit 0
