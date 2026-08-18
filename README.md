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

# 강사가 OpenRouter 키(sk-or-v1-...)를 배포한 경우에만 아래 한 줄을 더 실행합니다.
# export OPENAI_BASE_URL=https://openrouter.ai/api

cd step0-base && ./gradlew bootRun
# 브라우저에서 http://localhost:8080 을 엽니다
# 다음 단계로 넘어갈 때는 Ctrl+C 후
cd ../step1-chat && ./gradlew bootRun
```

## 실습 화면

각 단계마다 `http://localhost:8080`에 확인용 웹 화면이 있습니다. 단계가 올라갈수록 탭이 늘어납니다.

| 단계 | 화면에서 확인할 것 |
|---|---|
| step0 | 게시글 목록 조회와 새 글 등록 |
| step1 | 챗봇에 질문하고 답변 받기 |
| step2 | 같은 대화 ID로 앞말 기억, 요약을 JSON 구조로 받기 |
| step3 | 도구 호출로 인기글 조회, 카테고리 자동분류 |
| step4 | 댓글 등록 시 차단·순화·보류·통과 판정과 감사 로그 |
| step5 | 운영정책 문서 인덱싱 후 근거 기반 답변 |
| step6 | 위 기능을 한 체인으로 결합 |

외부 CDN을 쓰지 않으므로 사내망에서도 화면이 그대로 뜹니다.

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

## 실행 검증 (2026-08-18)

실제 API 키로 step0부터 step6까지 전 구간을 호출해 확인했습니다.

| 확인 항목 | 결과 |
|---|---|
| step0 게시판 CRUD | 조회·작성 정상 |
| step1 챗봇, 프롬프트 비교 | 정상 |
| step2 대화 기억, 구조화 요약 | 이름 회상·JSON 변환 정상 |
| step3 도구 호출, 자동분류 | 인기글 조회·카테고리 판정 정상 |
| step4 차단·순화·보류, 감사 로그 | 네 판정 모두 정상 |
| step5 RAG | 운영정책 3종 22청크, 근거 답변 정상 |
| step6 Advisor 결합 | 도구+RAG 동시 응답 정상 |

검증 중 발견해 고친 것은 `CHANGES.md`에 적었습니다.
