package com.frentis.board.web;

import com.frentis.board.domain.Post;
import com.frentis.board.domain.PostRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * step3: 카테고리 자동분류.
 *
 * 실습 확인용
 *   curl -s -X POST localhost:8080/api/classify -H 'Content-Type: application/json' \
 *        -d '{"title":"H2 콘솔이 안 열립니다","content":"404가 납니다. 경로 설정이 필요한가요?"}'
 *   curl -s -X POST localhost:8080/api/classify/reclassify-all
 */
@RestController
@RequestMapping("/api")
public class ClassifyController {

    private static final List<String> CATEGORIES = List.of("자유", "질문", "정보공유", "후기", "공지");

    private final ChatClient chatClient;
    private final PostRepository posts;

    public ClassifyController(ChatClient taskChatClient, PostRepository posts) {
        this.chatClient = taskChatClient;
        this.posts = posts;
    }

    @PostMapping("/classify")
    public Classification classify(@RequestBody ClassifyRequest request) {
        return classify(request.title(), request.content());
    }

    /** 기존 게시글 전체를 다시 분류한다. 분류가 바뀐 건만 저장한다. */
    @PostMapping("/classify/reclassify-all")
    public Map<String, Object> reclassifyAll() {
        int changed = 0;
        for (Post post : posts.findAll()) {
            Classification result = classify(post.getTitle(), post.getContent());
            if (!CATEGORIES.contains(result.category())) continue;
            if (result.category().equals(post.getCategory())) continue;
            if (result.confidence() < 0.7) continue;

            post.changeCategory(result.category());
            posts.save(post);
            changed++;
        }
        return Map.of("total", posts.count(), "changed", changed);
    }

    private Classification classify(String title, String content) {
        return chatClient.prompt()
                .system("""
                        게시글을 다음 다섯 카테고리 중 하나로 분류합니다.
                        자유: 잡담, 모임 공지가 아닌 일반 글
                        질문: 답을 구하는 글. 물음표가 없어도 도움을 요청하면 질문입니다.
                        정보공유: 방법, 팁, 자료를 알려 주는 글
                        후기: 직접 해 본 경험을 정리한 글
                        공지: 운영진의 안내

                        confidence는 0과 1 사이 값이며, 애매하면 낮게 줍니다.
                        reason은 한 문장으로 씁니다.
                        """)
                .user("제목: %s%n본문: %s".formatted(title, content))
                .call()
                .entity(Classification.class);
    }

    public record Classification(String category, double confidence, String reason) {}

    public record ClassifyRequest(String title, String content) {}
}
