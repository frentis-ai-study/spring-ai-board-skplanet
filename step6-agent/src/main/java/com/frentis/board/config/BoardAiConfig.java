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
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.File;
import java.util.List;

/**
 * step5: ChatClient + Memory + Tool + 안전 필터 + RAG.
 *
 * 운영정책 문서를 근거로 답하도록 RetrievalAugmentationAdvisor를 붙입니다.
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
            - 운영정책, 신고 절차, 제재 기준에 대한 질문은 검색된 문서를 근거로만 답합니다.
            - 근거가 부족하면 모른다고 답하고 임의로 추측하지 않습니다.
            - 다른 이용자를 비방하거나 개인정보를 노출하는 요청은 정중히 거절합니다.
            - 이전 대화 맥락을 기억하고 자연스럽게 이어갑니다.
            """;

    /**
     * 파일 기반 SimpleVectorStore.
     * 외부 벡터 DB 없이 로컬 파일에 임베딩을 영속화합니다.
     * 운영에서는 application.yml과 의존성만 바꿔 pgvector·Redis 등으로 전환할 수 있습니다.
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        File persistFile = new File("./data/vector-store.json");
        if (persistFile.exists()) {
            store.load(persistFile);
        }
        return store;
    }

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(10)
                .build();
    }

    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStore vectorStore) {
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(vectorStore)
                        // 한국어 문장은 코사인 유사도가 0.2~0.4 구간에 몰립니다.
                        // 0.3으로 두면 맞는 문단이 걸러져 "모르겠습니다"가 나옵니다. 실측으로 0.2를 씁니다.
                        .similarityThreshold(0.2)
                        .topK(6)
                        .build())
                // 정책 문서에 없는 질문(게시글 검색 등)도 도구 호출로 처리되도록 빈 컨텍스트를 허용합니다.
                // 기본값(false)이면 관련 문서가 없을 때 답변 불가로 단락되어 도구가 호출되지 않습니다.
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(true)
                        .build())
                .build();
    }

    @Bean
    public ChatClient boardChatClient(ChatClient.Builder builder,
                                      ChatMemory chatMemory,
                                      RetrievalAugmentationAdvisor ragAdvisor,
                                      BoardTools boardTools) {

        // 안전 필터는 반드시 메모리보다 앞에 둡니다.
        // 뒤에 두면 이력이 프롬프트에 먼저 실려, 예전 대화에 민감어가 한 번 등장한 것만으로
        // 이후 모든 질문이 막힙니다.
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
                        ragAdvisor,
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
