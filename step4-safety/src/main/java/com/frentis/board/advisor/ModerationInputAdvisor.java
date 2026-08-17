package com.frentis.board.advisor;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

/**
 * 입력 모더레이션 어드바이저.
 *
 * 사용자 메시지에 고위험 표현이 포함되면 LLM을 호출하지 않고 즉시 거절 응답으로 단락(short-circuit)합니다.
 * 체인 맨 앞(order가 가장 작은 값)에 두어야 차단할 요청에 임베딩·검색·도구 호출 비용이 발생하지 않습니다.
 *
 * 운영 환경에서는 이 자리에서 모더레이션 모델이나 사내 Safety 분류기를 호출합니다.
 */
public class ModerationInputAdvisor implements BaseAdvisor {

    /** 커뮤니티 정책상 즉시 차단하는 고위험 표현. */
    private static final List<String> BLOCKED_KEYWORDS = List.of(
            "폭탄 제조", "자살 방법", "마약 구매", "불법 촬영물",
            "주민등록번호 알려", "타인 계정 해킹"
    );

    private final int order;

    public ModerationInputAdvisor() {
        this(Integer.MIN_VALUE + 1000);
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
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        String userText = request.prompt().getInstructions().stream()
                .filter(m -> m instanceof UserMessage)
                .map(m -> ((UserMessage) m).getText())
                .reduce("", (a, b) -> a + " " + b);

        String normalized = TextNormalizer.normalize(userText);

        for (String banned : BLOCKED_KEYWORDS) {
            if (normalized.contains(TextNormalizer.normalize(banned))) {
                request.context().put("moderation.blocked", true);
                request.context().put("moderation.reason", banned);
                break;
            }
        }
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        if (!Boolean.TRUE.equals(response.context().get("moderation.blocked"))) {
            return response;
        }

        String reason = String.valueOf(response.context().getOrDefault("moderation.reason", "policy"));
        String message = "해당 요청은 커뮤니티 정책상 처리할 수 없습니다. (사유: %s)".formatted(reason);

        // finishReason을 명시해 두면 클라이언트가 "모델이 답을 못 한 것"과 "정책으로 막은 것"을 구분할 수 있습니다.
        ChatGenerationMetadata metadata = ChatGenerationMetadata.builder()
                .finishReason("STOP_BY_MODERATION")
                .build();

        ChatResponse safeResponse = new ChatResponse(
                List.of(new Generation(new AssistantMessage(message), metadata)));

        return ChatClientResponse.builder()
                .chatResponse(safeResponse)
                .context(response.context())
                .build();
    }
}
