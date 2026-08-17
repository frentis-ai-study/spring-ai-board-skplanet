package com.frentis.board.web;

import com.frentis.board.domain.*;
import com.frentis.board.moderation.ContentModerationService;
import com.frentis.board.moderation.ContentModerationService.ModerationResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * step4: 안전 필터링이 적용된 글쓰기 경로와 감사 로그 조회.
 *
 * 실습 확인용
 *   # 정상 글
 *   curl -s -X POST localhost:8080/api/safe/comments -H 'Content-Type: application/json' \
 *        -d '{"postId":1,"memberId":2,"content":"좋은 정리 감사합니다"}'
 *   # 순화 대상
 *   curl -s -X POST localhost:8080/api/safe/comments -H 'Content-Type: application/json' \
 *        -d '{"postId":1,"memberId":2,"content":"이런 멍청한 설정을 왜 쓰는지 모르겠네요"}'
 *   # 차단 대상
 *   curl -s -X POST localhost:8080/api/safe/comments -H 'Content-Type: application/json' \
 *        -d '{"postId":1,"memberId":2,"content":"계정 판매 문의 주세요"}'
 *   # 감사 로그
 *   curl -s localhost:8080/api/moderation/logs | jq
 */
@RestController
@RequestMapping("/api")
public class ModerationController {

    private final ContentModerationService moderation;
    private final PostRepository posts;
    private final CommentRepository comments;
    private final MemberRepository members;
    private final ModerationLogRepository logs;

    public ModerationController(ContentModerationService moderation, PostRepository posts,
                                CommentRepository comments, MemberRepository members,
                                ModerationLogRepository logs) {
        this.moderation = moderation;
        this.posts = posts;
        this.comments = comments;
        this.members = members;
        this.logs = logs;
    }

    /** 판정만 해 보는 엔드포인트. 저장하지 않는다. */
    @PostMapping("/moderation/check")
    public ModerationResult check(@RequestBody CheckRequest request) {
        return moderation.moderate("PREVIEW", request.memberId(), request.content());
    }

    @PostMapping("/safe/comments")
    public ResponseEntity<?> writeComment(@RequestBody CommentRequest request) {
        ModerationResult result = moderation.moderate("COMMENT", request.memberId(), request.content());

        if (result.blocked()) {
            members.findById(request.memberId()).ifPresent(m -> {
                m.addSanction();
                members.save(m);
            });
            return ResponseEntity.badRequest().body(Map.of(
                    "action", "BLOCK",
                    "reason", result.reason(),
                    "message", "커뮤니티 정책에 어긋나 등록되지 않았습니다."));
        }

        Comment comment = new Comment(request.postId(), request.memberId(),
                result.processedText() == null ? request.content() : result.processedText());

        if (result.held()) {
            comment.hold();
        }
        Comment saved = comments.save(comment);

        return ResponseEntity.ok(Map.of(
                "action", result.action(),
                "status", saved.getStatus(),
                "content", saved.getContent(),
                "logId", result.logId()));
    }

    @PostMapping("/safe/posts")
    public ResponseEntity<?> writePost(@RequestBody PostRequest request) {
        ModerationResult result = moderation.moderate("POST", request.memberId(), request.content());

        if (result.blocked()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "action", "BLOCK",
                    "reason", result.reason(),
                    "message", "커뮤니티 정책에 어긋나 등록되지 않았습니다."));
        }

        Post post = new Post(request.memberId(), request.title(),
                result.processedText() == null ? request.content() : result.processedText(),
                request.category() == null ? "자유" : request.category());

        if (result.held()) {
            post.hold();
        }
        Post saved = posts.save(post);

        return ResponseEntity.ok(Map.of(
                "action", result.action(),
                "status", saved.getStatus(),
                "postId", saved.getId(),
                "logId", result.logId()));
    }

    @GetMapping("/moderation/logs")
    public List<ModerationLog> listLogs(@RequestParam(required = false) String action) {
        return action == null ? logs.findAll() : logs.findByActionOrderByCreatedAtDesc(action);
    }

    /** 보류된 글 목록. 운영자 검토 화면에 해당한다. */
    @GetMapping("/moderation/held")
    public Map<String, Object> listHeld() {
        return Map.of(
                "posts", posts.findByStatus("HELD"),
                "logs", logs.findByActionOrderByCreatedAtDesc("HOLD"));
    }

    public record CheckRequest(Long memberId, String content) {}

    public record CommentRequest(Long postId, Long memberId, String content) {}

    public record PostRequest(Long memberId, String title, String content, String category) {}
}
