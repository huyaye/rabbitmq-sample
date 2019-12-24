# RabbitMQ 따라잡기 - 소스 코드

## 7장. 운영 환경에 RabbitMQ 설정하기

### Java

- `mvn exec:java`을 입력해서 애플리케이션 시뮬레이터를 시작하자.
- 애플리케이션 재연결 및 복구를 확인하기 위해 클러스터에서 RabbitMQ 브로커를 멈추고(stop) 재시작(restart)한다.
- 엔터(Enter)를 입력해서 애플리케이션을 중단할 수 있다.


### Ruby

- `bundle install` 명령어로 실행한다.
- 하나의 브로커에 연결하고 다른 브로커로 fail over 하기 위해 `src/main/ruby/connection_failover.rb`를 사용한다.


