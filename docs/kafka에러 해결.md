## 1. 자동 쿠버네티스 sh 파일 실행 시 에러
> zookeeper 상태가<br/>
> zookeeper-0        0/1     Running
---
```bash
readinessProbe:
  exec:
    command:
    - sh
    - -c
    - 'echo "ruok" | nc localhost 2181 | grep imok'
```
여기에서 nc(netcat)이 없을 가능성이 높음
### 아래 2번 해결방법대로 실행

## 2. 테스트 환경 스크립트 파일 실행시 에러
> The StatefulSet "zookeeper" is invalid: spec.template.spec.containers[0].readinessProbe.tcpSocket: Forbidden: may not specify more than 1 handler type
---
> readinessProbe/livenessProbe
> Prob 는 하나의 핸들러만 가질 수 잇음.
> - exec
> - httpGet
> - tcpSocket
> - grpc
---
```bash
readinessProbe:
    $patch: replace
    tcpSocket:
    port: 2181
    initialDelaySeconds: 10
    periodSeconds: 5
    timeoutSeconds: 3
    failureThreshold: 3
```
처럼 $path로 덮어버리거나 base의 statefulset을
```bash
readinessProbe:
    tcpSocket:
    port: 2181
    initialDelaySeconds: 10
    periodSeconds: 5
    timeoutSeconds: 3
    failureThreshold: 3
```
로 설정

## 3. 지속적으로 zookeeper는 실행되는데, kafka-0 이 0/1 상태
> 에러로그도 존재하지 않음.<br/>
> 로그가 "port is deprecated..." 이후 아무것도 출력 없이 종료됨.<br/>
> 이건 Confluent의 /etc/confluent/docker/run 스크립트가 설정 파일을 생성하다가 조용히 실패<br/>
> 지속적인 에러메세지 발생<br/>
> 에러 : port is deprecated. Please use KAFKA_ADVERTISED_LISTENERS instead.<br/>
> 환경변수 로그 확인 시,<br/>
> KAFKA_PORT=tcp://10.110.32.18:9092 로 PORT설정 에러<br/>

### 수행 1
> Confluent Kafka 이미지는 다음 환경 변수가 필수
> - KAFKA_BROKER_ID 
> - KAFKA_ZOOKEEPER_CONNECT 
> - KAFKA_ADVERTISED_LISTENERS 
> - KAFKA_LISTENER_SECURITY_PROTOCOL_MAP 
> - KAFKA_INTER_BROKER_LISTENER_NAME 
> <br/>여전히 동작안함.

### 수행 2
> Kubernetes는 Service가 있으면 자동으로 환경 변수를 생성한다고 함
> enableServiceLinks 비활성화
```bash
# kafka-standalone-patch.yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: kafka
  namespace: shop-msa
spec:
  replicas: 1
  template:
    spec:
      # Service 환경 변수 자동 생성 비활성화
      enableServiceLinks: false
```

## 4. redis-sentinel Failed to resolve hostname 'redis-master-0.redis-master.shop-msa.svc.cluster.local'
```bash
# redis-master 의 hostname 확인
kubectl exec -it redis-master-0 -n shop-msa -- nslookup redis-master

# 알고보니 문제는 주로 Kubernetes 내부 DNS가 Redis 노드 이름을 IP로 변환하지 못해 발생했던것
# sentinel resolve-hostnames yes 항목 추가
data:
  redis-sentinel.conf: |
    port 26379
    sentinel resolve-hostnames yes
```