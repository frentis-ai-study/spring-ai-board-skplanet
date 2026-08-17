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

import java.io.File;
import java.util.List;

/**
 * step5: ChatClient + Memory + Tool + 안전 필터 + RAG.
 *
 * 운영정책 문서를 근거로 답하도록 RetrievalAugmentationAdvisor를 붙입니다.
 */
@Configuration
public class BoardAiConfig {

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
                        // 한국어 임베딩은 코사인 유사도가 낮게 나오는 경향이 있어 0.3을 권장합니다
                        .similarityThreshold(0.3)
                        .topK(4)
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

        SafeGuardAdvisor safeGuard = SafeGuardAdvisor.builder()
                .sensitiveWords(List.of("주민등록번호", "신용카드번호", "비밀번호", "계좌번호"))
                .build();

        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(
                        new ModerationInputAdvisor(),
                        safeGuard,
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        ragAdvisor,
                        new SimpleLoggerAdvisor()
                )
                .defaultTools(boardTools)
                .build();
    }
}
