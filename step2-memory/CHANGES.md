# CHANGES — step2-memory (vs step1-chat)

## 추가

- `src/main/java/com/frentis/board/web/SummaryController.java` — Structured Output 실습 (`/api/summarize`, `/api/summarize-batch`, `/api/posts/ai-draft`)

## 변경

- `build.gradle.kts` — `spring-ai-starter-model-chat-memory-repository-jdbc` 추가
- `config/BoardAiConfig.java` — `ChatMemory` 빈, `MessageChatMemoryAdvisor` 기본 적용
- `web/ChatController.java` — `conversationId` 지원
- `application.yml` — `spring.ai.chat.memory.repository.jdbc.initialize-schema: always`

## 변경 없음

- 도메인 / `web/BoardController.java`
