package com.frentis.board.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * step1: 가장 단순한 ChatClient. 메모리·도구·RAG 없음.
 */
@Configuration
public class BoardAiConfig {

    static final String SYSTEM_PROMPT = """
            당신은 개발자 커뮤니티 게시판의 도우미입니다.
            - 항상 한국어 격식체로 답합니다.
            - 게시글 작성, 검색, 커뮤니티 이용 안내를 돕습니다.
            - 확실하지 않은 내용은 모른다고 답합니다.
            """;

    @Bean
    public ChatClient boardChatClient(ChatClient.Builder builder) {
        return builder.defaultSystem(SYSTEM_PROMPT).build();
    }
}
