package com.frentis.board.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 안전 필터링 이력.
 *
 * 원문을 남기지 않으면 이용자의 이의 제기에 대응할 수 없고 정책도 개선할 수 없습니다.
 * 다만 원문에 개인정보가 포함될 수 있으므로, 운영에서는 보존 기간과 접근 권한을 함께 정해야 합니다.
 */
@Entity
@Table(name = "moderation_logs")
public class ModerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** POST 또는 COMMENT */
    @Column(nullable = false, length = 20)
    private String targetType;

    private Long targetId;

    private Long memberId;

    @Column(nullable = false, length = 4000)
    private String originalText;

    @Column(length = 4000)
    private String processedText;

    /** PASS, REWRITE, HOLD, BLOCK */
    @Column(nullable = false, length = 20)
    private String action;

    @Column(length = 500)
    private String reason;

    @Column(length = 100)
    private String decidedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected ModerationLog() {}

    public ModerationLog(String targetType, Long targetId, Long memberId,
                         String originalText, String processedText,
                         String action, String reason, String decidedBy) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.memberId = memberId;
        this.originalText = originalText;
        this.processedText = processedText;
        this.action = action;
        this.reason = reason;
        this.decidedBy = decidedBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public Long getMemberId() { return memberId; }
    public String getOriginalText() { return originalText; }
    public String getProcessedText() { return processedText; }
    public String getAction() { return action; }
    public String getReason() { return reason; }
    public String getDecidedBy() { return decidedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
