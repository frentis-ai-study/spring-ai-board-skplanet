package com.frentis.board.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 커뮤니티 회원. */
@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    /** NEWBIE, REGULAR, VETERAN, STAFF */
    @Column(nullable = false, length = 20)
    private String grade;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    /** 누적 제재 횟수. 안전 필터링 실습에서 사용한다. */
    @Column(nullable = false)
    private Integer sanctionCount;

    protected Member() {}

    public Member(String nickname, String grade) {
        this.nickname = nickname;
        this.grade = grade;
        this.joinedAt = LocalDateTime.now();
        this.sanctionCount = 0;
    }

    public void addSanction() {
        this.sanctionCount++;
    }

    public Long getId() { return id; }
    public String getNickname() { return nickname; }
    public String getGrade() { return grade; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public Integer getSanctionCount() { return sanctionCount; }
}
