# rpsuh
```
> RPUSH another_list "bar" "baz"
(integer) 2

# Appending multiple elements to an existing list
> RPUSH another_list "foo" "bar" "baz"
(integer) 5
```
- 리스트 오른쪽에 파라미터 순서대로 원소 추가
- 리스트가 없으면 새로 생성후 추가
- 추가된 후 리스트 길이 반환


# lpush
- 리스트 왼쪽에 원소 추가
- 리스트가 없으면 새로 생성후 추가

```
> LPUSH list_key "a" "b" "c"
(integer) 3

> LRANGE list_key 0 -1
1) "c"
2) "b"
3) "a"
```

## test

```
$ redis-cli
> LPUSH list_key "c"
# Expect: (integer) 1

> LPUSH list_key "b" "a"
# Expect: (integer) 3
```


```
> LRANGE list_key 0 -1
# Expect RESP Encoded Array: ["a", "b", "c"]
```


# lrange
## positive index
```
# Create a list with 5 items
> RPUSH list_key "a" "b" "c" "d" "e"
(integer) 5

# List first 2 items 
> LRANGE list_key 0 1
1) "a"
2) "b"

# List items from indexes 2-4
> LRANGE list_key 2 4
1) "c"
2) "d"
3) "e"
```

## negative index
```
# Create a list with 5 items
> RPUSH list_key "a" "b" "c" "d" "e"
(integer) 5

# List last 2 items 
> LRANGE list_key -2 -1
1) "d"
2) "e"

# List all items expect last 2
> LRANGE list_key 0 -3
1) "a"
2) "b"
3) "c"
```


## test

```
$ redis-cli RPUSH list_key "a" "b" "c" "d" "e"
```

```
$ redis-cli LRANGE list_key 0 2
# Expect RESP Encoded Array: ["a", "b", "c"]
```

```
*3\r\n
$1\r\n
a\r\n
$1\r\n
b\r\n
$1\r\n
c\r\n
```

- start > end
  - 텅빈 리스트 처리

- list가 없을 경우

```
*0\r\n
```
- list 범위 넘겨 요청
    - end가 넘어갈 경우 error 없이 가능 범위 최대치 까지 지원
    - start가 길이 이상일 경우 error 없이 empty list 처리

- start, end 가 음수인 경우
  - start = list.size + start
  - end = list.size + end
  - 음수 값이 list.size 보다 크면 0으로 대체



# llen
- 길이 반환
- 없는 리스트는 0 반환


# lpop
- 앞에 원소 제거후 반환 bulk string
- 파라미터 지정 없으면 왼쪽 한개 제거
- 파라미터 지정시 지정 수만큼 왼쪽에서 제거해 array 반환
- 리스트 길이보다 길게 지정하면 리스트 전체 제거

```
> RPUSH list_key "a" "b" "c" "d"
(integer) 4

> LPOP list_key
"a"

> RPUSH list_key "a" "b" "c" "d"
(integer) 4

> LPOP list_key 2
1) "a"
2) "b"
```

- 리스트 없으면

```
# nil
$-1\r\n
```

# blpop
- 리스트 앞에 값 제거해 값을 [list key, 제거한 값] array 반환
- 리스트가 없으면 제거할 수 있는 값이 추가되어 생길 떄가지 대기
- 파라미터로 대기 초를 지정
- 0으로 지정하면 무한 대기
- 대기 시간 넘어 가면 nil 반환


## 고민 사항
- 대기 시키는 법?

