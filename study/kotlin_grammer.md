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

# coroutine

```xml

<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-coroutines-core</artifactId>
    <version>1.8.1</version>
</dependency>
```

```kotlin
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

val waitMs = 100
val deferred = CompletableDeferred<String>()
val ret = withTimeoutOrNull(waitMs) { deferred.await() } // 지정 ms 만큼 대기
if (ret == null) {
    // 시간 초과
    return
}
// != null은 complete 호출 됨을 의미
```

- suspend 를 붙인 함수를 호출하는 함수는 suspend 가 있어야 한다
- 그럼 최상위 main 까지도 suspend를 붙이나?

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    handler.handle()
}
```


# Mutex
- 코루틴 안에서 thread safe 하게 코드 작성하기 위한 장치
