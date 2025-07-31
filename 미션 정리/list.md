# lrange

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

- list가 없을 경우

```
*0\r\n
```
- list 범위 넘겨 요청
    - end가 넘어갈 경우 error 없이 가능 범위 최대치 까지 지원
    - start가 넘어갈 경우 error 없이 empty list 처리
