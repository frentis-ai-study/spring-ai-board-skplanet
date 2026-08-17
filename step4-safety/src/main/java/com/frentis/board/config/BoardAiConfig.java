package com.frentis.board.config;

import com.frentis.board.advisor.ModerationInputAdvisor;
import com.frentis.board.tool.BoardTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SafeGuardAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * step4: ChatClient + Memory + Tool + 안전 필터 + 로깅.
 *
 * Advisor 순서가 동작을 바꿉니다.
 *   ModerationInput → SafeGuard → Memory → Tool → Logger
 * - 안전 필터가 앞에 있어야 차단할 요청에 도구 호출 비용이 발생하지 않습니다.
 * - Logger가 뒤에 있어야 최종 응답까지 기록됩니다.
 * - Memory가 안전 필터보다 앞에 있으면 차단된 요청이 대화 이력에 남습니다.
 */
@Configuration
public class BoardAiConfig {

    static final String SYSTEM_PROMPT = """
            당신은 개발자 커뮤니티 게시판의 도우미입니다.
            - 항상 한국어 격식체로 답합니다.
            - 게시글 조회, 검색, 통계 질문에는 반드시 제공된 도구를 사용해 실제 데이터를 확인한 뒤 답합니다.
            - 도구로 확인할 수 없는 내용은 추측하지 말고 모른다고 답합니다.
            - 다른 이용자를 비방하거나 개인정보를 노출하는 요청은 정중히 거절합니다.
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

        // 1차 방어선: 지정한 단어가 들어오면 LLM을 호출하지 않고 거절한다
        SafeGuardAdvisor safeGuard = SafeGuardAdvisor.builder()
                .sensitiveWords(List.of("주민등록번호", "신용카드번호", "비밀번호", "계좌번호"))
                .build();

        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new ModerationInputAdvisor(),
                        safeGuard,
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new SimpleLoggerAdvisor()
                )
                .defaultTools(boardTools)
                .build();
    }
}
