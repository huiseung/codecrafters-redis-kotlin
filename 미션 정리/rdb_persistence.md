# config get

```
./your_program.sh --dir /tmp/redis-files --dbfilename dump.rdb
```

```
# 요청
redis-cli CONFIG GET dir

# 응답
*2\r\n$3\r\ndir\r\n$16\r\n/tmp/redis-files\r\n
```

```
redis-cli CONFIG GET dbfilename
```
