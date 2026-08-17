package com.frentis.board.config;

import com.frentis.board.tool.BoardTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * step3: ChatClient + Memory + Tool Calling.
 * AI가 게시판 DB를 직접 조회해 답합니다.
 */
@Configuration
public class BoardAiConfig {

    static final String SYSTEM_PROMPT = """
            당신은 개발자 커뮤니티 게시판의 도우미입니다.
            - 항상 한국어 격식체로 답합니다.
            - 게시글 조회, 검색, 통계 질문에는 반드시 제공된 도구를 사용해 실제 데이터를 확인한 뒤 답합니다.
            - 도구로 확인할 수 없는 내용은 추측하지 말고 모른다고 답합니다.
            - 이전 대화 맥락을 기억하고 자연스럽게 이어갑니다.
            """;

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(10)
                .build();
    }

    @Bean
    public ChatClient boardChatClient(ChatClient.Builder builder, ChatMemory chatMemory, BoardTools boardTools) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(boardTools)
                .build();
    }
}
