## 8. VS Code 에서 실행 (인자값 전달)

터미널이 아닌 **VS Code의 Run 버튼으로 실행할 경우**에는
프로그램에 전달할 인자값을 별도로 설정해야 한다.

VS Code에서는 실행 시 전달할 값을
**`launch.json` 파일**로 관리한다.

---

### 1) launch.json 생성

1. VS Code 왼쪽의 **Run and Debug** 아이콘 클릭
2. **launch.json 파일만들기** 선택
3. 환경 선택에서 **Java** 선택

그러면 프로젝트에 다음 파일이 생성된다.

```text
.vscode/
└─ launch.json
```

---

### 2) launch.json에 인자값 설정

생성된 `launch.json` 파일을 열고
`args` 항목에 실행 시 전달할 값을 입력한다.

```json
{
  // launch.json 파일의 스키마 버전
  // VS Code 디버거 설정 형식의 버전이다
  "version": "0.2.0",

  // 디버깅 실행 설정 목록
  "configurations": [
    {
      // 사용할 디버거 타입
      // Java 프로젝트이므로 "java"
      "type": "java",

      // VS Code 실행 메뉴에 표시될 이름
      "name": "실습디버깅",

      // 실행 방식
      // launch = 새 JVM을 띄워서 실행
      "request": "launch",

      // main 메서드를 가진 클래스 이름
      // public static void main(String[] args)가 있는 클래스
      "mainClass": "App",

      // main 메서드로 전달되는 실행 인자
      // args[0] == "hello"
      "args": "hello"
    }
  ]
}
```

이 상태에서 **Run App**으로 실행하면
자바 프로그램에는 다음과 같이 값이 전달된다.

```java
args[0] == "hello"
```

---

### 3) 여러 값 전달하기

공백으로 구분해 여러 인자값을 전달할 수도 있다.

```json
"args": "hello world"
```

실행 시 자바에서는 다음과 같이 들어온다.

```java
args[0] == "hello"
args[1] == "world"
```

---