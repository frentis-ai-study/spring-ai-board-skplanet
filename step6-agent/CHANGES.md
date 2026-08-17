# CHANGES — step6-agent (vs step5-rag)

## 추가

- `config/ResilienceConfig.java` — OpenAI 호출 Retry + CircuitBreaker
- `scheduled/ChatMemoryCleanupJob.java` — 대화 이력 보존 기간(90일) 정리 잡
- `web/ScenarioController.java` — 종합 시나리오 6종 일괄 실행 (`/api/scenarios/run`)

## 변경

- `BoardApplication.java` — `@EnableScheduling`
- `build.gradle.kts` — resilience4j, micrometer-prometheus 추가
- `application.yml` — actuator에 metrics, prometheus 노출

## 최종 Advisor 체인

```
ModerationInput → SafeGuard → Memory → RAG → Logger
                                              + Tools
```

## 실습 포인트

- `/api/scenarios/run`으로 여섯 시나리오가 각각 의도대로 동작하는지 한 번에 확인합니다
- 로그에서 Advisor 실행 순서와 도구 호출 내역을 확인합니다
- `/actuator/metrics/gen_ai.client.operation`으로 토큰 사용량을 확인합니다
