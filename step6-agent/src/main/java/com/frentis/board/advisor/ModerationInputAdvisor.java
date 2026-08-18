package com.frentis.board.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.core.Ordered;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 입력 모더레이션 어드바이저.
 *
 * <p>고위험 표현이 들어오면 모델을 호출하지 않고 그 자리에서 거절 응답을 돌려줍니다.
 * 체인의 다음 어드바이저로 넘기지 않으므로 임베딩·검색·도구 호출 비용이 발생하지 않고,
 * 뒤에 있는 대화 메모리도 실행되지 않아 차단된 문장이 이력에 남지 않습니다.
 *
 * <p>검사 대상은 <b>이번에 들어온 사용자 메시지 하나</b>입니다.
 * 프롬프트 전체를 검사하면 대화 이력까지 함께 읽게 되어,
 * 예전에 한 번 걸린 대화는 이후 모든 질문이 계속 차단됩니다.
 *
 * <p>운영 환경에서는 이 자리에서 모더레이션 모델이나 사내 Safety 분류기를 호출합니다.
 */
public class ModerationInputAdvisor implements CallAdvisor, StreamAdvisor {

    /** 커뮤니티 정책상 즉시 차단하는 고위험 표현. */
    private static final List<String> BLOCKED_KEYWORDS = List.of(
            "폭탄 제조", "자살 방법", "마약 구매", "불법 촬영물",
            "주민등록번호 알려", "타인 계정 해킹"
    );

    private final int order;

    public ModerationInputAdvisor() {
        this(Ordered.HIGHEST_PRECEDENCE + 100);
    }

    public ModerationInputAdvisor(int order) {
        this.order = order;
    }

    @Override
    public String getName() {
        return "ModerationInputAdvisor";
    }

    @Override
    public int getOrder() {
        return this.order;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String reason = findBlockedKeyword(request);
        if (reason != null) {
            return refusal(reason);
        }
        return chain.nextCall(request);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        String reason = findBlockedKeyword(request);
        if (reason != null) {
            return Flux.just(refusal(reason));
        }
        return chain.nextStream(request);
    }

    /** 이번 사용자 메시지에서 차단 표현을 찾는다. 없으면 null. */
    private String findBlockedKeyword(ChatClientRequest request) {
        String current = currentUserText(request);
        if (current == null || current.isBlank()) {
            return null;
        }
        String normalized = TextNormalizer.normalize(current);
        for (String banned : BLOCKED_KEYWORDS) {
            if (normalized.contains(TextNormalizer.normalize(banned))) {
                return banned;
            }
        }
        return null;
    }

    /** 프롬프트에서 마지막 사용자 메시지를 꺼낸다. 이력이 아니라 이번 질문만 본다. */
    private String currentUserText(ChatClientRequest request) {
        List<Message> messages = request.prompt().getInstructions();
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage user) {
                return user.getText();
            }
        }
        return null;
    }

    private ChatClientResponse refusal(String reason) {
        String message = "해당 요청은 커뮤니티 정책상 처리할 수 없습니다. (사유: %s)".formatted(reason);

        // finishReason을 명시해 두면 클라이언트가 "모델이 답을 못 한 것"과 "정책으로 막은 것"을 구분할 수 있습니다.
        ChatGenerationMetadata metadata = ChatGenerationMetadata.builder()
                .finishReason("STOP_BY_MODERATION")
                .build();

        ChatResponse response = new ChatResponse(
                List.of(new Generation(new AssistantMessage(message), metadata)));

        return ChatClientResponse.builder().chatResponse(response).build();
    }
}
