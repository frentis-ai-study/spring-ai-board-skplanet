package com.frentis.board.web;

import com.frentis.board.domain.Post;
import com.frentis.board.domain.PostRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * step2: Structured Output 실습.
 * 게시글 본문을 넣으면 정형 데이터로 받아 그대로 DB에 저장합니다.
 *
 * 실습 확인용
 *   curl -s -X POST localhost:8080/api/summarize -H 'Content-Type: application/json' \
 *        -d '{"content":"JPA에서 N+1이 계속 발생합니다. fetch join을 써도 페이징이 깨져서 고민입니다."}'
 */
@RestController
@RequestMapping("/api")
public class SummaryController {

    private final ChatClient chatClient;
    private final PostRepository posts;

    public SummaryController(ChatClient taskChatClient, PostRepository posts) {
        this.chatClient = taskChatClient;
        this.posts = posts;
    }

    /** 단일 객체 매핑. */
    @PostMapping("/summarize")
    public PostSummary summarize(@RequestBody SummarizeRequest request) {
        return chatClient.prompt()
                .system("""
                        게시글 본문을 분석해 요약 정보를 만듭니다.
                        category는 자유, 질문, 정보공유, 후기, 공지 중 하나만 사용합니다.
                        sentiment는 긍정, 중립, 부정 중 하나만 사용합니다.
                        keywords는 3개 이하로 뽑습니다.
                        """)
                .user(request.content())
                .call()
                .entity(PostSummary.class);
    }

    /** List 타입 매핑. 여러 건을 한 번에 분류할 때 쓴다. */
    @PostMapping("/summarize-batch")
    public List<PostSummary> summarizeBatch(@RequestBody BatchRequest request) {
        return chatClient.prompt()
                .system("각 본문마다 요약 정보를 하나씩 만듭니다. 입력 순서를 유지합니다.")
                .user(String.join("\n---\n", request.contents()))
                .call()
                .entity(new ParameterizedTypeReference<List<PostSummary>>() {});
    }

    /** 구조화 결과를 그대로 엔티티에 저장하는 파이프라인. */
    @PostMapping("/posts/ai-draft")
    public Post saveFromDraft(@RequestBody DraftRequest request) {
        PostSummary summary = summarize(new SummarizeRequest(request.content()));
        return posts.save(new Post(request.memberId(), summary.title(), request.content(), summary.category()));
    }

    /**
     * LLM 응답을 매핑할 구조.
     * record의 필드명과 주석이 그대로 JSON Schema로 전달되므로, 이름을 뜻이 통하게 지어야 정확도가 올라간다.
     */
    public record PostSummary(
            String title,
            String category,
            List<String> keywords,
            String sentiment,
            String oneLineSummary
    ) {}

    public record SummarizeRequest(String content) {}

    public record BatchRequest(List<String> contents) {}

    public record DraftRequest(Long memberId, String content) {}
}
