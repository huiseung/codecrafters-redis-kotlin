# port 설정
- 실행 파라미터 --port 6380 로 포트 설정
- default 6379

# info command

```

```


# Full Resynchronization
### 전송 과정
```
Replica -> Master: PSYNC
Master -> Replica: FULLRESYNC
Master: RDB file 생성
Master -> Replica: RDB file byte stream 
```[RedisCloneFull.kt](..%2F..%2F..%2FOther_Code%2FRedisCloneFull.kt)

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


## Write Synchronization
- Master의 쓰기 요청은 Replica로 전송하고 Master에게 결과를 응답하지 않는다

### 고민 사항
- 명령어 처리시 일반 Client로 부터 쓰기인지 Master로 부터 쓰기 요청인지 구분하는 방법은?
  - clientSocket 타입을 변경
    - normal_connect
      - 요청에 대해 명령을 처리하고 응답을 쓴다
    - master_connect
      - 쓰기 요청에 대해서는 응답을 쓰지 않는다
    - rdb_receive
      - 요청 파싱시 rdb file 파싱을 호출한다
