#!/bin/bash

echo "========================================="
echo "Minikube 재시작"
echo "========================================="

echo "1. Minikube 상태 확인..."
MINIKUBE_STATUS=$(minikube status --format='{{.Host}}')

if [ "$MINIKUBE_STATUS" = "Paused" ]; then
    echo "2. Minikube unpause..."
    minikube unpause
elif [ "$MINIKUBE_STATUS" = "Stopped" ]; then
    echo "2. Minikube start..."
    minikube start
else
    echo "2. Minikube 이미 실행 중"
fi

echo "3. Pod 스케일 업..."
kubectl scale statefulset postgresql --replicas=1 -n shop-msa
kubectl scale statefulset redis-master --replicas=1 -n shop-msa
kubectl scale statefulset redis-replica --replicas=1 -n shop-msa
kubectl scale statefulset redis-sentinel --replicas=1 -n shop-msa
kubectl scale statefulset zookeeper --replicas=1 -n shop-msa
kubectl scale statefulset kafka --replicas=1 -n shop-msa

echo "4. Pod Ready 대기..."
kubectl wait --for=condition=Ready pod -l app=postgresql -n shop-msa --timeout=120s
kubectl wait --for=condition=Ready pod -l app=redis -n shop-msa --timeout=120s
kubectl wait --for=condition=Ready pod -l app=zookeeper -n shop-msa --timeout=120s
kubectl wait --for=condition=Ready pod -l app=kafka -n shop-msa --timeout=120s

echo ""
echo "✅ 모든 서비스 시작 완료!"
kubectl get pods -n shop-msa