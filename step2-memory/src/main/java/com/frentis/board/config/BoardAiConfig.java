package com.frentis.board.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * step2: ChatClient + Memory(JDBC).
 * conversationId 별로 대화 이력을 유지합니다.
 */
@Configuration
public class BoardAiConfig {

    static final String SYSTEM_PROMPT = """
            당신은 개발자 커뮤니티 게시판의 도우미입니다.
            - 항상 한국어 격식체로 답합니다.
            - 게시글 작성, 검색, 커뮤니티 이용 안내를 돕습니다.
            - 이전 대화 맥락을 기억하고 자연스럽게 이어갑니다.
            - 확실하지 않은 내용은 모른다고 답합니다.
            """;

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(10)
                .build();
    }

    @Bean
    public ChatClient boardChatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /**
     * 단발 작업용 ChatClient.
     *
     * 요약·분류·유해성 판정·순화처럼 "한 번 묻고 한 번 받는" 호출에 씁니다.
     * 대화용(boardChatClient)과 반드시 분리해야 하는 이유는 두 가지입니다.
     *   1) 대화 메모리 어드바이저는 conversationId를 요구합니다. 단발 호출에는 그 값이 없습니다.
     *   2) 판정 대상 문장에 민감어가 들어 있으면 안전 필터가 판정 호출 자체를 막아 버립니다.
     * 판정기는 판정 대상을 그대로 읽어야 하므로 필터를 걸지 않습니다.
     */
    @Bean
    public ChatClient taskChatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
