package com.frentis.board.web;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * step2: 대화 메모리를 적용한 챗봇.
 * conversationId를 넘기면 같은 세션의 이전 대화를 기억합니다.
 *
 * 실습 확인용
 *   curl -s -X POST localhost:8080/api/chat -H 'Content-Type: application/json' \
 *        -d '{"conversationId":"u-1","message":"제 닉네임은 판교개발자입니다"}'
 *   curl -s -X POST localhost:8080/api/chat -H 'Content-Type: application/json' \
 *        -d '{"conversationId":"u-1","message":"제 닉네임이 뭐라고 했죠?"}'
 *   # conversationId를 u-2로 바꾸면 기억하지 못하는 것을 확인합니다.
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
        String conversationId = request.conversationId() == null ? "default" : request.conversationId();
        String content = chatClient.prompt()
                .user(request.message())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
        return Map.of("conversationId", conversationId, "content", content);
    }

    @PostMapping("/chat-meta")
    public ChatResponse chatWithMeta(@RequestBody ChatRequest request) {
        return chatClient.prompt().user(request.message()).call().chatResponse();
    }

    @PostMapping("/chat-compare")
    public Map<String, String> chatWithCustomSystem(@RequestBody CompareRequest request) {
        String content = chatClient.prompt()
                .system(request.systemPrompt())
                .user(request.message())
                .call()
                .content();
        return Map.of("systemPrompt", request.systemPrompt(), "content", content);
    }

    public record ChatRequest(String conversationId, String message) {}

    public record CompareRequest(String systemPrompt, String message) {}
}
