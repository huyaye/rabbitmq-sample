# RabbitMQ 따라잡기 - 소스 코드

## 4장. 애플리케이션 로그 처리하기

### 파이썬(python)

- 리눅스에서 'pip'와 의존성을 설치한다.

    sudo pip install -r requirements.txt

- 로그 저장소를 시작한다.

    `src/main/python/logs-archiver.py`

- 오류 보고를 시작한다.

    `src/main/python/logs-error-reporter.py`


### JMeter

 `jmeter/lib_ext` 디렉토리는 JMeter의 설치에 필요한 JAR 파일을 포함한다.
 `jmeter/logs-load-test.jmx` 파일은 'access-logs' 익스체인지에 임의의 Apache2 로그 메시지 50,000개를 보내는 부하 테스트를 포함한다.
