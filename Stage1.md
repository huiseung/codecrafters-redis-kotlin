# RESP
- 레디스와 통신하기 위해 정의한 프로토콜

|기호| 의미                               | 예                                                        |
|---|----------------------------------|----------------------------------------------------------|
| + | simple string 단순 문자열             | +PONG\r\n                                                |
| $ | bulk string(문자열의 바이트 길이와 내용을 표현) | 보통 문자: $5\r\nhello\r\n, 빈 문자: $0\r\n\r\n, nil: $-1\r\n   |
| : | 숫자                               | :1000\r\n                                                |
| - | 오류                               | -ERR unkown command\r\n                                  | 
| * | 배열                               | 길이2인 bulk string array: *2\r\n$3\r\nfoo\r\n$3\r\nbar\r\n |

- 배열의 원소 타입으로는 simple string(+), bulk string($), error(-), integer(:), array(*) 가능하다
- 모든 요청은 bulk string array 로 전송한다
- 레디스가 지원하는 다양한 자료구조들을 다 위 기호로 표현 가능 하다
- 개행은 "\r\n"으로 구분한다
- 마지막은 "\r\n" 으로 끝나야 한다

# PING Command

```
request

*1\r\n$4\r\nPING\r\n
```


```
response


+PONG\r\n
```


# SET  Command

```
request

SET foo bar
```

```
response

+OK\r\n
```


# GET Command

```
request

get foo
```

```
response

없을 경우
$-1\r\n

있을 경우
$3\r\nbar\r\n

```

# Expiry


```
request

ms 단위 만료 시간 
SET foo bar px 100
```

```
response

+OK\r\n
```
