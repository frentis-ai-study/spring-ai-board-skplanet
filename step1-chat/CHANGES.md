# CHANGES — step1-chat (vs step0-base)

## 추가

- `src/main/java/com/frentis/board/config/BoardAiConfig.java` — ChatClient Bean
- `src/main/java/com/frentis/board/web/ChatController.java` — `/api/chat`, `/api/chat-meta`, `/api/chat-compare`

## 변경

- `build.gradle.kts` — `spring-ai-starter-model-openai` 추가, `spring-ai-bom` 1.1.7 import
- `src/main/resources/application.yml` — `spring.ai.openai.*` 설정 추가

## 변경 없음

- 도메인(`Member`, `Post`, `Comment`, `*Repository`, `DataSeeder`)
- `web/BoardController.java`
