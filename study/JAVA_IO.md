# InputStream
- byte 단위 읽기 추상 클래스
- int read()
  - 입력 장치에서 1 byte 크기만큼 데이터를 읽는다
  - 읽은 바이트 값
  - 없으면 -1

# Reader
- char 단위 읽기 추상 클래스

## InputStreamReader
- Stream을 Reader로 변경하는 브릿지 패턴 클래스
- 생성자에 할당하는 Stream이 입력장치다
  - socket.getInputStream(): 네트워크에서 데이터 읽음
  - FileInputStrem(): 파일에서 데이터 읽음
  - System.in: 표준 입력에서 데이터 읽음
- int read()
  - 입력 장치에서 1 char 크기만큼 데이터를 읽는다
  - 읽은 문자 코드
  - 없으면 -1

## BufferedReader
- char[] 필드가 있는 다른 Reader 클래스의 데코레이터 패턴 클래스
- String readLine()
  - 줄바꿈(\n, \r, \r\n) 을 만날 때까지 문자를 읽어 String으로 반환
  - 줄바꿈은 결과에 포함하지 않는다
  - 읽기 끝에 도달시 null 반환

-----------

# ServerSocket
- Socket accept()
  - 클라이언트로 부터 연결을 수락
  - 클라이언트와 연결하는 Socket을 반환
  - 다수의 클라이언트와 연결하고 싶다면 while(true)문 안에서 호출해 반환한 Socket을 별도 thread에서 동작
