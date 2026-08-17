# Spring AI 지능형 웹 실무 — 실습 저장소 (SK플래닛)

2026-08-19 SK플래닛 Tech조직 실무교육 "Spring AI 지능형 웹 실무" 수강생용 단계별 실습 저장소입니다.
하나의 **커뮤니티 게시판** 서비스에 AI 기능을 한 단계씩 얹어 가면서 Spring AI를 익힙니다.

각 폴더는 독립적으로 빌드·실행 가능한 완전한 Spring Boot 프로젝트입니다. 앞 단계가 밀리더라도 다음 폴더를 열면 바로 따라올 수 있습니다.

## 사전 준비

- **Java 21** (Temurin/Adoptium 권장)
- **OpenAI API 키** — 환경변수 `OPENAI_API_KEY`로 주입 (강사 제공)
- (선택) `curl`, `jq` 등 REST 호출 도구

> 별도의 데이터베이스 서버나 Docker는 필요하지 않습니다. 첫 부팅 시 `./data/boarddb.mv.db` H2 파일이 자동 생성됩니다.

## 단계 구성

| 단계 | 폴더 | 추가되는 것 | 시간표 |
|------|------|------------|--------|
| 0 | `step0-base` | 게시판 도메인 + REST CRUD (AI 없음) | 11:00-11:25 |
| 1 | `step1-chat` | + ChatClient 게시판 도우미 챗봇 | 11:25-13:25 |
| 2 | `step2-memory` | + Structured Output(게시글 요약 구조화) + JDBC ChatMemory | 13:25-13:50 |
| 3 | `step3-tools` | + Function Calling(게시글 조회·검색·통계) + 카테고리 자동분류 | 14:25-14:50 |
| 4 | `step4-safety` | + 금지어 필터 + 모더레이션 + 순화 + 감사 로깅 | 15:25-15:50 |
| 5 | `step5-rag` | + SimpleVectorStore RAG (운영정책 문서 3종) | 16:25-16:50 |
| 6 | `step6-agent` | + Advisor Chain 결합 + 프로덕션 설정 | 17:00-17:25 |

## 실행

```bash
export OPENAI_API_KEY=sk-...

cd step0-base && ./gradlew bootRun
# 다음 단계로 넘어갈 때는 Ctrl+C 후
cd ../step1-chat && ./gradlew bootRun
```

## 도메인 모델

| 엔티티 | 내용 |
|--------|------|
| `Member` | 회원 (닉네임, 등급, 가입일, 누적 제재 횟수) |
| `Post` | 게시글 (제목, 본문, 카테고리, 상태, 조회수, 신고수) |
| `Comment` | 댓글 (본문, 상태) |
| `ModerationLog` | 필터링 이력 (원문, 변환문, 조치, 사유) — step4부터 |

카테고리는 `자유 / 질문 / 정보공유 / 후기 / 공지` 5종이며, step3에서 AI가 자동 판정합니다.
상태는 `PUBLISHED / HELD / DELETED` 3종이며, step4 안전 필터링에서 `HELD`로 보류 처리합니다.

## 공통 인프라

- **H2 파일 모드** — 외부 DB 서버 없이 `./data/boarddb` 파일에 영속화
- **JDBC ChatMemory** — Spring AI 자동 구성으로 `SPRING_AI_CHAT_MEMORY` 테이블 생성 (step2 이상)
- **SimpleVectorStore** — 외부 벡터 DB 없이 `./data/vector-store.json`에 임베딩 영속화 (step5 이상)
- Spring Boot 3.5.0 / Spring AI 1.1.7
- 패키지 베이스: `com.frentis.board.{config, domain, web, tool, rag, advisor, moderation}`

> Spring AI 2.0.0이 2026-06-12 GA로 나왔습니다(Spring Boot 4 baseline, MCP SDK 2.0, composable tool-calling advisor).
> 본 실습은 검증된 1.1.7 기준이며, 2.0 변경점은 강의 마지막 블록에서 다룹니다.
