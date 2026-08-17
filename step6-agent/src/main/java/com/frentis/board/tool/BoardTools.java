package com.frentis.board.tool;

import com.frentis.board.domain.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring AI Tool Calling — 커뮤니티 게시판 도메인 함수.
 *
 * 권한 검증 정책
 * - 본 학습용 코드는 호출자 ID(callerMemberId)를 도구 파라미터로 명시적으로 받습니다.
 * - 운영 환경에서는 SecurityContextHolder에서 인증 주체를 꺼내 검증해야 합니다.
 *   LLM이 채워 주는 값을 권한 판단에 그대로 쓰면 안 됩니다.
 */
@Component
public class BoardTools {

    private final MemberRepository members;
    private final PostRepository posts;
    private final CommentRepository comments;

    public BoardTools(MemberRepository members, PostRepository posts, CommentRepository comments) {
        this.members = members;
        this.posts = posts;
        this.comments = comments;
    }

    public record PostView(Long id, String title, String category, String status,
                           int viewCount, int reportCount, String writtenAt) {
        static PostView from(Post p) {
            return new PostView(p.getId(), p.getTitle(), p.getCategory(), p.getStatus(),
                    p.getViewCount(), p.getReportCount(), p.getCreatedAt().toLocalDate().toString());
        }
    }

    public record MemberView(Long id, String nickname, String grade, int sanctionCount) {}

    public record MemberActivity(String nickname, long postCount, long commentCount, long totalViews) {}

    @Tool(description = "닉네임으로 회원 정보를 조회한다. 회원 ID, 등급, 누적 제재 횟수를 반환한다.")
    public MemberView findMember(
            @ToolParam(description = "회원 닉네임 (예: 판교개발자)") String nickname) {
        return members.findByNickname(nickname)
                .map(m -> new MemberView(m.getId(), m.getNickname(), m.getGrade(), m.getSanctionCount()))
                .orElse(null);
    }

    @Tool(description = "제목 또는 본문에 키워드가 포함된 게시글을 검색한다. 최신순으로 반환한다.")
    public List<PostView> searchPosts(
            @ToolParam(description = "검색할 키워드 (예: JPA, 코드 리뷰)") String keyword) {
        return posts.searchByKeyword(keyword).stream().map(PostView::from).limit(10).toList();
    }

    @Tool(description = "카테고리별 게시글 목록을 조회한다. 카테고리는 자유, 질문, 정보공유, 후기, 공지 중 하나다.")
    public List<PostView> listPostsByCategory(
            @ToolParam(description = "카테고리명") String category) {
        return posts.findByCategoryOrderByCreatedAtDesc(category).stream().map(PostView::from).limit(10).toList();
    }

    @Tool(description = "최근 며칠 동안 조회수가 높은 인기 게시글을 조회한다.")
    public List<PostView> findPopularPosts(
            @ToolParam(description = "며칠 전부터 집계할지 (예: 7이면 최근 7일)") int days) {
        return posts.findPopularSince(LocalDateTime.now().minusDays(days))
                .stream().map(PostView::from).limit(5).toList();
    }

    @Tool(description = "특정 회원이 쓴 게시글 목록을 조회한다. 본인 것만 조회할 수 있다.")
    public List<PostView> getMyPosts(
            @ToolParam(description = "조회 대상 회원 ID") Long memberId,
            @ToolParam(description = "호출자(인증된 사용자) 회원 ID. 운영 환경은 SecurityContext에서 추출.") Long callerMemberId) {
        if (!memberId.equals(callerMemberId)) {
            throw new SecurityException("FORBIDDEN: 본인이 작성한 글만 조회할 수 있습니다.");
        }
        return posts.findByMemberIdOrderByCreatedAtDesc(memberId).stream().map(PostView::from).toList();
    }

    @Tool(description = "회원의 활동 통계를 조회한다. 작성 게시글 수, 댓글 수, 누적 조회수를 반환한다.")
    public MemberActivity getMemberActivity(
            @ToolParam(description = "회원 닉네임") String nickname) {
        Member member = members.findByNickname(nickname).orElse(null);
        if (member == null) return null;

        List<Post> myPosts = posts.findByMemberIdOrderByCreatedAtDesc(member.getId());
        long totalViews = myPosts.stream().mapToLong(Post::getViewCount).sum();
        long commentCount = comments.findByMemberIdOrderByCreatedAtDesc(member.getId()).size();

        return new MemberActivity(member.getNickname(), myPosts.size(), commentCount, totalViews);
    }

    @Tool(description = "게시글의 카테고리를 변경한다. 자동분류 결과를 반영할 때 사용한다.")
    @Transactional
    public PostView changeCategory(
            @ToolParam(description = "게시글 ID") Long postId,
            @ToolParam(description = "새 카테고리 (자유, 질문, 정보공유, 후기, 공지 중 하나)") String category) {
        Post post = posts.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다: " + postId));
        post.changeCategory(category);
        return PostView.from(posts.save(post));
    }
}
