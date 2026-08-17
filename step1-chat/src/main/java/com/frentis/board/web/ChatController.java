package com.frentis.board.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * step1: 단발 챗봇 호출. Memory 없음.
 *
 * 실습 확인용
 *   curl -s -X POST localhost:8080/api/chat -H 'Content-Type: application/json' \
 *        -d '{"message":"게시글 제목을 잘 쓰는 방법 알려주세요"}'
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient boardChatClient) {
        this.chatClient = boardChatClient;
    }

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody ChatRequest request) {
        String content = chatClient.prompt().user(request.message()).call().content();
        return Map.of("content", content);
    }

    /** 토큰 사용량·종료 사유 등 메타데이터까지 확인할 때 사용한다. */
    @PostMapping("/chat-meta")
    public ChatResponse chatWithMeta(@RequestBody ChatRequest request) {
        return chatClient.prompt().user(request.message()).call().chatResponse();
    }

    /**
     * 프롬프트 비교 실습용.
     * 같은 질문에 시스템 메시지만 바꿔 넣어 응답이 어떻게 달라지는지 확인한다.
     */
    @PostMapping("/chat-compare")
    public Map<String, String> chatWithCustomSystem(@RequestBody CompareRequest request) {
        String content = chatClient.prompt()
                .system(request.systemPrompt())
                .user(request.message())
                .call()
                .content();
        return Map.of("systemPrompt", request.systemPrompt(), "content", content);
    }

    public record ChatRequest(String message) {}

    public record CompareRequest(String systemPrompt, String message) {}
}
