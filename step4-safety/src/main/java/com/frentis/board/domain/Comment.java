package com.frentis.board.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 댓글. */
@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long postId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 1000)
    private String content;

    /** PUBLISHED, HELD, DELETED */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Comment() {}

    public Comment(Long postId, Long memberId, String content) {
        this.postId = postId;
        this.memberId = memberId;
        this.content = content;
        this.status = "PUBLISHED";
        this.createdAt = LocalDateTime.now();
    }

    public void rewrite(String content) {
        this.content = content;
    }

    public void hold() {
        this.status = "HELD";
    }

    public Long getId() { return id; }
    public Long getPostId() { return postId; }
    public Long getMemberId() { return memberId; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
