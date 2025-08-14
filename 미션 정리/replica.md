# port 설정
- 실행 파라미터 --port 6380 로 포트 설정
- default 6379

# info command

```
info replication
```

- 한줄 씩 bulk string
```
role:master
master_repid:8371b4fb1155b71f4a04d3e1bc3e18c4a990aeeb
master_repl_offset:0
```

# handshake
## PING

```

```

## REPLCONF 
- 2번의 replconf 명령어 요청을 보낸다

- request
```
*3\r\n$8\r\nREPLCONF\r\n$14\r\nlistening-port\r\n$4\r\n6380\r\n
```
- response
```
+OK\r\n
```


- request
```
REPLCONF capa psync2
```

- response
```
+OK\r\n
```

## PSYNC
- request
```
*3\r\n$5\r\nPSYNC\r\n$1\r\n?\r\n$2\r\n-1\r\n

```
- response

```
+FULLRESYNC {replId} 0\r\n 
```

# Full Resynchronization
### 전송 과정
```
Replica -> Master: PSYNC
Master -> Replica: FULLRESYNC
Master: RDB file 생성
Master -> Replica: RDB file byte stream 
```

### RDB file byte stream
```
${file_size}\r\n
{file_bytes}\r\n
```

### 고민 사항
- 일반 요청이랑 RDB file 요청이 형태가 다른데 어떻게 구분해서 전송/파싱을 구현하지?
  - clientSocket에 타입을 부여한다
    - normal 타입은 명령어를 파싱
    - Recive_RDB 타입은 rdb file 파싱


# Write Synchronization
- Master의 쓰기 요청은 Replica로 전송하고 Master에게 결과를 응답하지 않는다

### 고민 사항
- 명령어 처리시 일반 Client로 부터 쓰기인지 Master로 부터 쓰기 요청인지 구분하는 방법은?
  - clientSocket 타입을 변경


# REPLCONF GETACK
- master 가 replica 쓰기 명령을 전파할 때 replica는 쓰기 전파에 대한 응답을 master 전송하지 않는다
- master는 replconf getack 명령어를 replica에게 보내 replica가 처리한 명령 offset을 받아 동기화 상태를 판단한다
- offset은 replica가 받은 요청들의 resp 표현 바이트 누적값이다
- master->replica에게 replica가 살아있음 확인 용으로 ping을 보내 응답 기대는 안하고 offset 만 늘리는 요청을 보낼 수 있다

# wait
```
wait {num replica} {timeout}
```
- master-replica간 쓰기 명령 동기화 확인 명령어, 보장이 아니라 확인이다
- num replica: 기다릴 레플리카 수
- timeout: 최대 대기 시간(초)
- 클라이언트가 master가 해당 명령을 보내면 master는 replica의 offset
