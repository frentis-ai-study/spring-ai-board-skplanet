# CHANGES — step3-tools (vs step2-memory)

## 추가

- `src/main/java/com/frentis/board/tool/BoardTools.java` — @Tool 7종 (회원 조회, 게시글 검색, 카테고리별 조회, 인기글, 내 글, 활동 통계, 카테고리 변경)
- `src/main/java/com/frentis/board/web/ClassifyController.java` — 카테고리 자동분류 (`/api/classify`, `/api/classify/reclassify-all`)

## 변경

- `config/BoardAiConfig.java` — `defaultTools(boardTools)` 추가, 시스템 프롬프트에 도구 사용 지침 추가

## 변경 없음

- 도메인 / `BoardController` / `ChatController` / `SummaryController`

## 실습 질의 예시

```
"JPA 관련 글 찾아줘"
"최근 7일 인기 게시글 알려줘"
"판교개발자님 활동 통계 보여줘"
"질문 카테고리에 어떤 글들이 있어?"
```
