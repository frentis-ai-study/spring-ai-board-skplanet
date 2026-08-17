package com.frentis.board.domain;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** 데모용 초기 데이터를 1회 적재한다. */
@Component
class DataSeeder implements CommandLineRunner {

    private final MemberRepository members;
    private final PostRepository posts;
    private final CommentRepository comments;

    DataSeeder(MemberRepository members, PostRepository posts, CommentRepository comments) {
        this.members = members;
        this.posts = posts;
        this.comments = comments;
    }

    @Override
    public void run(String... args) {
        if (members.count() > 0) return;

        Member jinwoo = members.save(new Member("판교개발자", "VETERAN"));
        Member sora = members.save(new Member("소라소라", "REGULAR"));
        Member newbie = members.save(new Member("첫줄", "NEWBIE"));

        Post p1 = posts.save(new Post(jinwoo.getId(),
                "Spring Boot 3.5 업그레이드 후기",
                "사내 서비스 12개를 3.4에서 3.5로 올렸습니다. 가장 크게 걸린 건 Jackson 설정과 Actuator 엔드포인트 경로였고, "
                        + "테스트 컨테이너 버전도 함께 올려야 했습니다. 롤백 계획을 먼저 세워두니 마음이 편했습니다.",
                "후기"));

        Post p2 = posts.save(new Post(sora.getId(),
                "JPA N+1 문제 어떻게 잡으시나요",
                "fetch join으로 해결하다 보니 쿼리가 점점 커집니다. @EntityGraph와 배치 사이즈 중에 어느 쪽을 기본으로 두시는지 궁금합니다.",
                "질문"));

        Post p3 = posts.save(new Post(jinwoo.getId(),
                "사내 개발자 스터디 모집합니다",
                "매주 수요일 저녁에 분산 시스템 책을 같이 읽습니다. 6명까지 받고, 발표는 돌아가면서 합니다.",
                "자유"));

        Post p4 = posts.save(new Post(newbie.getId(),
                "H2 콘솔이 안 열립니다",
                "application.yml에 h2 console enabled true를 넣었는데 404가 납니다. 경로 설정을 따로 해야 하나요?",
                "질문"));

        posts.save(new Post(sora.getId(),
                "코드 리뷰 문화 정착시킨 방법 공유",
                "리뷰어를 2명으로 고정하고, 하루 안에 응답하지 못하면 자동으로 다음 사람에게 넘어가도록 규칙을 만들었습니다. "
                        + "리뷰 대기 시간이 평균 이틀에서 네 시간으로 줄었습니다.",
                "정보공유"));

        comments.save(new Comment(p1.getId(), sora.getId(), "롤백 계획 먼저 세운다는 말에 공감합니다."));
        comments.save(new Comment(p1.getId(), newbie.getId(), "Jackson 설정 어떤 부분이 걸렸는지 더 알려주실 수 있을까요?"));
        comments.save(new Comment(p2.getId(), jinwoo.getId(), "저는 @EntityGraph를 기본으로 두고, 복잡해지면 fetch join으로 갑니다."));
        comments.save(new Comment(p4.getId(), jinwoo.getId(), "spring.h2.console.path도 함께 확인해 보세요."));
    }
}
