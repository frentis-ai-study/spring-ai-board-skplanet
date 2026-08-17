# CHANGES — step0-base

시작점입니다. AI 기능은 없습니다.

## 구성

- `domain/Member.java`, `Post.java`, `Comment.java` — 커뮤니티 게시판 도메인
- `domain/*Repository.java` — 조회 메서드 (키워드 검색, 카테고리별, 인기글)
- `domain/DataSeeder.java` — 회원 3명, 게시글 5건, 댓글 4건 초기 적재
- `web/BoardController.java` — 게시판 REST API

## 확인

```bash
./gradlew bootRun
curl -s localhost:8080/api/posts | jq
curl -s localhost:8080/api/members | jq
```

`./data/boarddb.mv.db` 파일이 생성되면 정상입니다.
