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
