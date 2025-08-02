# use

- java의 try-wit-resources와 유사
- closeable

# thread

- java의 Thread 인스턴스 생성, start 메서드 호출

# sealed

- 닫힌 계승 구조를 만드는 키워드

## sealed class

- 같은 파일 안에서만 상속 가능한 abstract class
- 컴파일 단계에서 when문 조건의 가능한 값들을 알기에 else 없이 사용 가능

# mutable list

- 코틀린의 MutableList는 java의 ArrayList를 이용한다

# return, parameter is call by value but ...

- java와 kotlin은 heap 공간의 객체를 가리키는 참조 변수를 복사해 넘긴다
- 참조변수를 이용해 객체를 조작할 수 있다
    - 함수 안에서 파라미터로 받은 객체를 조작하면 함수 밖에서도 유지 된다
    - 클래스 필드를 함수의 반환 값으로 가져온 객체를 조작하면 클래스 필드 값도 변환되어 있다

# java Condition
- await
- await(time:Long, unit: TimeUnit)

## time_await spurious wakeup 문제
- 가짜 깨움 문제
- 정확하게 지정한 시간에 깨지 않고 먼저 깨는 문제 
