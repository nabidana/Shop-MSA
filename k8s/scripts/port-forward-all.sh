#!/bin/bash

# Port Forward 자동화 스크립트
# 백엔드 개발을 위해 인프라 포트를 로컬로 포워딩

echo "========================================="
echo "  로컬 Port Forward 시작"
echo "========================================="
echo ""

# 기존 Port Forward 프로세스 확인 및 종료
echo "🔍 기존 Port Forward 프로세스 확인..."
if pgrep -f "kubectl port-forward" > /dev/null; then
    echo "⚠️  기존 Port Forward 프로세스가 실행 중입니다."
    read -p "기존 프로세스를 종료하시겠습니까? (y/N): " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        pkill -f "kubectl port-forward"
        echo "✅ 기존 프로세스 종료 완료"
        sleep 2
    else
        echo "❌ 취소되었습니다."
        exit 1
    fi
fi

echo ""
echo "🚀 Port Forward 시작..."
echo ""

# PostgreSQL Port Forward
echo "📦 PostgreSQL: localhost:5432"
kubectl port-forward svc/postgresql 5432:5432 -n shop-msa > /dev/null 2>&1 &
POSTGRES_PID=$!

sleep 1

# Redis Port Forward
echo "📦 Redis: localhost:6379"
kubectl port-forward svc/redis-master 6379:6379 -n shop-msa > /dev/null 2>&1 &
REDIS_PID=$!

sleep 1

# Kafka Port Forward
echo "📦 Kafka: localhost:9092"
kubectl port-forward svc/kafka 9092:9092 -n shop-msa > /dev/null 2>&1 &
KAFKA_PID=$!

sleep 1

# Redis Sentinel Port Forward (선택사항)
echo "📦 Redis Sentinel: localhost:26379"
kubectl port-forward svc/redis-sentinel 26379:26379 -n shop-msa > /dev/null 2>&1 &
SENTINEL_PID=$!

echo ""
echo "========================================="
echo "  ✅ Port Forward 완료"
echo "========================================="
echo ""
echo "📝 연결 정보:"
echo "  PostgreSQL: localhost:5432"
echo "  Redis:      localhost:6379"
echo "  Kafka:      localhost:9092"
echo ""
echo "🔗 Spring Boot application.yml 설정:"
echo ""
echo "spring:"
echo "  datasource:"
echo "    url: jdbc:postgresql://localhost:5432/userdb"
echo "    username: userservice"
echo "    password: test_user_password"
echo "  data:"
echo "    redis:"
echo "      host: localhost"
echo "      port: 6379"
echo "  kafka:"
echo "    bootstrap-servers: localhost:9092"
echo ""
echo "========================================="
echo "⚠️  이 터미널을 종료하면 Port Forward도 종료됩니다."
echo "   종료하려면 Ctrl+C를 누르거나"
echo "   다른 터미널에서 다음 명령 실행:"
echo "   pkill -f 'kubectl port-forward'"
echo "========================================="
echo ""

# Cleanup function
cleanup() {
    echo ""
    echo "🛑 Port Forward 종료 중..."
    kill $POSTGRES_PID $REDIS_PID $KAFKA_PID 2>/dev/null
    kill $SENTINEL_PID 2>/dev/null  # Sentinel 사용 시
    echo "✅ 모든 Port Forward가 종료되었습니다."
    exit 0
}

# Trap Ctrl+C
trap cleanup SIGINT SIGTERM

# 대기 (무한 대기)
echo "⏳ Port Forward 실행 중... (종료: Ctrl+C)"
while true; do
    sleep 1
    # Process 상태 확인
    if ! kill -0 $POSTGRES_PID 2>/dev/null || \
       ! kill -0 $REDIS_PID 2>/dev/null || \
       ! kill -0 $KAFKA_PID 2>/dev/null; then
        echo ""
        echo "⚠️  Port Forward 프로세스가 종료되었습니다."
        echo "재시작하려면 스크립트를 다시 실행하세요."
        exit 1
    fi
done
