#!/bin/bash

echo "========================================="
echo "Minikube 일시정지"
echo "========================================="

echo "1. Pod 스케일 다운 중..."
kubectl scale statefulset --all --replicas=0 -n shop-msa

echo "2. Pod 종료 대기 중..."
kubectl wait --for=delete pod --all -n shop-msa --timeout=120s

echo "3. Minikube 일시정지..."
minikube pause

echo "✅ 일시정지 완료!"
minikube status