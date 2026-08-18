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
import org.springframework.core.Ordered;

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

    /**
     * Advisor 실행 순서. 숫자가 작을수록 먼저 실행됩니다.
     *
     * 모더레이션 → 안전 필터 → 대화 메모리 순서를 지켜야 합니다.
     * 메모리가 앞에 오면 대화 이력이 프롬프트에 실린 뒤 필터가 검사하게 되어,
     * 한 번 민감어가 오간 대화는 그 뒤 모든 질문이 계속 차단됩니다.
     */
    static final int MODERATION_ORDER = Ordered.HIGHEST_PRECEDENCE + 100;
    static final int GUARD_ORDER = Ordered.HIGHEST_PRECEDENCE + 200;
    static final int MEMORY_ORDER = Ordered.HIGHEST_PRECEDENCE + 1000;

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

        // 1차 방어선: 지정한 단어가 들어오면 LLM을 호출하지 않고 거절한다.
        // order를 메모리보다 앞으로 당기는 것이 핵심입니다. 뒤에 두면 이력이 프롬프트에 먼저 실려,
        // 예전 대화에 민감어가 한 번 등장한 것만으로 이후 모든 질문이 막힙니다.
        SafeGuardAdvisor safeGuard = SafeGuardAdvisor.builder()
                .sensitiveWords(List.of("주민등록번호", "신용카드번호", "비밀번호", "계좌번호"))
                .failureResponse("민감정보가 포함된 요청은 처리하지 않습니다. 개인정보를 빼고 다시 물어봐 주십시오.")
                .order(GUARD_ORDER)
                .build();

        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new ModerationInputAdvisor(MODERATION_ORDER),
                        safeGuard,
                        MessageChatMemoryAdvisor.builder(chatMemory).order(MEMORY_ORDER).build(),
                        new SimpleLoggerAdvisor()
                )
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
