package com.frentis.board.moderation;

import com.frentis.board.advisor.TextNormalizer;
import com.frentis.board.domain.ModerationLog;
import com.frentis.board.domain.ModerationLogRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 게시글·댓글 본문에 대한 안전 필터링.
 *
 * 처리 방식을 세 가지로 나눕니다.
 *   BLOCK   — 명백한 위법·고위험. 저장하지 않습니다.
 *   HOLD    — 판단이 애매함. 보류 상태로 저장하고 운영자가 확인합니다.
 *   REWRITE — 표현은 거칠지만 내용은 정상. 순화해서 저장합니다.
 *
 * 차단만 쓰면 이용자가 이탈합니다. 순화와 보류를 함께 설계해야 서비스가 굴러갑니다.
 */
@Service
public class ContentModerationService {

    /** 즉시 차단 대상. */
    private static final List<String> BLOCK_WORDS = List.of(
            "폭탄 제조", "마약 판매", "불법 촬영물", "계정 판매"
    );

    /** 순화 대상. 실제 운영에서는 사전을 외부 설정으로 분리합니다. */
    private static final List<String> SOFTEN_WORDS = List.of(
            "멍청", "바보", "쓰레기", "꺼져", "닥쳐"
    );

    private final ChatClient chatClient;
    private final ModerationLogRepository logs;

    public ContentModerationService(ChatClient taskChatClient, ModerationLogRepository logs) {
        this.chatClient = taskChatClient;
        this.logs = logs;
    }

    /**
     * 본문을 검사하고 조치를 결정한다.
     * 판정 결과는 원문과 함께 반드시 기록한다.
     */
    public ModerationResult moderate(String targetType, Long memberId, String text) {
        String normalized = TextNormalizer.normalize(text);

        // 1단계: 금지어 사전 — 비용 0, 명백한 건만 잡는다
        for (String word : BLOCK_WORDS) {
            if (normalized.contains(TextNormalizer.normalize(word))) {
                return record(targetType, memberId, text, null, "BLOCK",
                        "금지어 포함: " + word, "keyword-filter");
            }
        }

        boolean needsSoftening = SOFTEN_WORDS.stream()
                .anyMatch(w -> normalized.contains(TextNormalizer.normalize(w)));

        // 2단계: 모델 판정 — 사전에 없는 표현을 잡는다
        Judgement judgement = judge(text);

        if (judgement.severity() >= 4) {
            return record(targetType, memberId, text, null, "HOLD",
                    "%s (심각도 %d)".formatted(judgement.category(), judgement.severity()), "llm-judge");
        }

        if (needsSoftening || judgement.severity() >= 2) {
            Rewritten rewritten = rewrite(text);
            if (rewritten.changed()) {
                return record(targetType, memberId, text, rewritten.text(), "REWRITE",
                        judgement.reason(), "llm-rewrite");
            }
        }

        return record(targetType, memberId, text, text, "PASS", null, "keyword-filter");
    }

    /** 유해성 판정. 운영에서는 모더레이션 모델이나 사내 분류기로 대체한다. */
    private Judgement judge(String text) {
        return chatClient.prompt()
                .system("""
                        커뮤니티 게시물의 유해성을 판정합니다.
                        category는 정상, 비속어, 인신공격, 혐오, 광고, 개인정보노출 중 하나입니다.
                        severity는 0에서 5 사이 정수입니다. 0은 문제 없음, 5는 즉시 삭제 대상입니다.
                        기술 토론에서 흔한 강한 표현(예: 이 코드는 최악이다)은 인신공격이 아닙니다.
                        reason은 한 문장으로 씁니다.
                        """)
                .user(text)
                .call()
                .entity(Judgement.class);
    }

    /** 순화. 의미와 논지는 유지하고 표현만 바꾼다. */
    public Rewritten rewrite(String text) {
        return chatClient.prompt()
                .system("""
                        커뮤니티 게시물을 순화합니다.
                        원문의 의미와 논지는 그대로 유지하고, 욕설·비하·인신공격 표현만 완곡하게 바꿉니다.
                        내용을 요약하거나 논지를 무디게 만들지 않습니다.
                        바꿀 것이 없으면 원문을 그대로 두고 changed를 false로 응답합니다.
                        note에는 무엇을 바꿨는지 한 문장으로 씁니다.
                        """)
                .user(text)
                .call()
                .entity(Rewritten.class);
    }

    private ModerationResult record(String targetType, Long memberId, String original,
                                    String processed, String action, String reason, String decidedBy) {
        ModerationLog log = logs.save(new ModerationLog(
                targetType, null, memberId, original, processed, action, reason, decidedBy));
        return new ModerationResult(action, processed, reason, log.getId());
    }

    public record Judgement(String category, int severity, String reason) {}

    public record Rewritten(String text, boolean changed, String note) {}

    public record ModerationResult(String action, String processedText, String reason, Long logId) {

        public boolean blocked() { return "BLOCK".equals(action); }

        public boolean held() { return "HOLD".equals(action); }
    }
}
