# CHANGES — step4-safety (vs step3-tools)

## 추가

- `domain/ModerationLog.java`, `domain/ModerationLogRepository.java` — 감사 로그
- `advisor/ModerationInputAdvisor.java` — 고위험 표현 입력 단락(short-circuit), `finishReason=STOP_BY_MODERATION`
- `advisor/TextNormalizer.java` — 자모 결합·반복 축약·특수문자 제거로 우회 표기 대응
- `moderation/ContentModerationService.java` — BLOCK / HOLD / REWRITE / PASS 판정과 순화
- `web/ModerationController.java` — 안전 필터가 적용된 글쓰기 경로, 감사 로그 조회

## 변경

- `config/BoardAiConfig.java`
  - Advisor 체인: `ModerationInput → SafeGuard → Memory → Logger`
  - `SafeGuardAdvisor`로 민감정보 단어 차단
  - `SimpleLoggerAdvisor` 추가

## 실습 포인트

- 같은 문장을 `/api/moderation/check`에 넣어 조치가 어떻게 갈리는지 확인합니다
- `TextNormalizer`를 끄고 초성체·특수문자 삽입으로 우회해 봅니다
- Advisor 순서를 바꿔(`SafeGuard`를 맨 뒤로) 차단된 요청에도 도구가 호출되는지 로그로 확인합니다
