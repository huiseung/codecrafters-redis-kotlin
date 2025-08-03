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

- 해당 파라미터가 없거나 해당하는 위치에 파일이 없으면 rdb file이 없는거다

# rdb file format
- byte sequence 로 이루어진 파일

## length encoding
- 첫 1바이트에 앞 2비트에 따른 구분

### 0b00
- 나머지 6비트 10진수로 표현한 값 = 다음 데이터를 이루는 바이트 수
- string이면 해당 바이트들이 문자

### 0b01
- 나머지 6비트 + 다음 1바이트 (big endian) = 다음 데이터를 이루는 바이트 수
- string이면 해당 바이트들이 문자

### 0b10
- 나머지 무시, 다음 4바이트 (big endian) = 다음 데이터를 이루는 바이트 수
- string이면 해당 바이트들이 문자

### 0b11 
- 0xC_, 0xD_, 0xE_, 0xF_ 로 시작
- 숫자를 저장한 경우를 의미
- 첫 바이트에 따른 인코딩 법
  - 0xC0: 다음 1바이트 int
  - 0xC1: 다음 2바이트 int(little endian)
  - 0xC2: 다음 4바이트 int(little endian)

  
## header section
- magic string: REDIS
- version number: 4자리 숫자

```
52 45 44 49 53 30 30 31 31 // ASCII: REDIS0011
```


## metadata section

```
FA    // 여기부터 metadata 입니다
09 72 65 64 69 73 2D 76 65 72  // string encode: redis-ver 
06 36 2E 30 2E 31 36 // string encode: 6.0.16
```


## database section

```
FE // 여기부터 database section 입니다
00  // 데이터 베이스 index 번호입니다

FB // 테이블 사이즈
03 // size encode: 만료 시간 없이 저장된 키 숫자
02 // size encode: 만료 시간 지정된 키 숫자


// 만료 시간 지정된 키 숫자 만큼 반복
FC // 만료 시간 지정 된 키 정보 ms
15 72 E7 07 8F 01 00 00 // 만료 시간 unsigned long, little endian(우측에서 좌측으로 읽기), Unix time
00 // value를 string encode
03 66 6F 6F // string encode: key
03 62 61 72 // string encode: value

FD // 만료 시간 지정 된 키 정보 sec
52 ED 2A 66 // 만료 시간 unsigned int, little endian, Unix time
00 // value 를 string encode
03 62 61 7A // key
03 71 75 78 // value

// 만료 시간 없이 저장된 키 숫자 만큼 반복
00   // value를 string encode 
06 66 6F 6F 62 61 72 // string encode: key
06 62 61 7A 71 75 78 // string encode: value

```

## end of file section
```
FF // 여기부터 end of file section
89 3b b7 4e f8 0f 77 19 // checksum 8 bytes
```

# save 
- 현재 상태에 대한 rdb file을 만든다
```
save
```

