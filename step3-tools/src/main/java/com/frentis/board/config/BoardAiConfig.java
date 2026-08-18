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
