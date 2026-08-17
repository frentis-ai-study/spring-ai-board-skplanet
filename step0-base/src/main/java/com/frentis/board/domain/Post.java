package com.frentis.board.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 게시글. */
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String content;

    /** 자유, 질문, 정보공유, 후기, 공지 — step3에서 AI가 자동 판정한다. */
    @Column(nullable = false, length = 20)
    private String category;

    /** PUBLISHED, HELD, DELETED — step4 안전 필터링에서 사용한다. */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private Integer viewCount;

    @Column(nullable = false)
    private Integer reportCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Post() {}

    public Post(Long memberId, String title, String content, String category) {
        this.memberId = memberId;
        this.title = title;
        this.content = content;
        this.category = category;
        this.status = "PUBLISHED";
        this.viewCount = 0;
        this.reportCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    public void changeCategory(String category) {
        this.category = category;
    }

    public void hold() {
        this.status = "HELD";
    }

    public void publish() {
        this.status = "PUBLISHED";
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void report() {
        this.reportCount++;
    }

    public Long getId() { return id; }
    public Long getMemberId() { return memberId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }
    public Integer getViewCount() { return viewCount; }
    public Integer getReportCount() { return reportCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
