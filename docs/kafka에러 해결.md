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
