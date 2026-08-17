# CHANGES — step5-rag (vs step4-safety)

## 추가

- `rag/PolicyIndexer.java` — 정책 문서를 섹션 단위로 청킹해 VectorStore에 적재
- `web/RagController.java` — `/api/rag/index`
- `src/main/resources/docs/community-policy.txt` — 커뮤니티 운영정책
- `src/main/resources/docs/report-process.txt` — 신고·처리 절차
- `src/main/resources/docs/faq.txt` — 자주 묻는 질문

## 변경

- `build.gradle.kts` — `spring-ai-advisors-vector-store`, `spring-ai-rag` 추가
- `config/BoardAiConfig.java` — `VectorStore`, `RetrievalAugmentationAdvisor` 빈 추가, 체인에 RAG 삽입
- `application.yml` — `board.rag.policies-path`, `board.rag.persist-path`

## 실습 포인트

- 인덱싱 전후로 같은 질문을 던져 답이 어떻게 달라지는지 비교합니다
- `similarityThreshold`를 0.3에서 0.7로 올려 검색이 안 되는 것을 확인합니다 (한국어 임베딩 특성)
- `allowEmptyContext`를 false로 바꾸면 도구 호출까지 막히는 것을 확인합니다
