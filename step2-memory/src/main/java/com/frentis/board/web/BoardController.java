package com.frentis.board.web;

import com.frentis.board.domain.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 커뮤니티 게시판 REST 엔드포인트 (AI 미사용).
 * 단계별 학습에서 도메인 데이터가 정상 적재되었는지 확인하는 용도.
 */
@RestController
@RequestMapping("/api")
public class BoardController {

    private final MemberRepository members;
    private final PostRepository posts;
    private final CommentRepository comments;

    public BoardController(MemberRepository members, PostRepository posts, CommentRepository comments) {
        this.members = members;
        this.posts = posts;
        this.comments = comments;
    }

    @GetMapping("/members")
    public List<Member> listMembers() {
        return members.findAll();
    }

    @GetMapping("/members/{id}")
    public Optional<Member> getMember(@PathVariable Long id) {
        return members.findById(id);
    }

    @GetMapping("/posts")
    public List<Post> listPosts(@RequestParam(required = false) String category) {
        return category == null
                ? posts.findAll()
                : posts.findByCategoryOrderByCreatedAtDesc(category);
    }

    @GetMapping("/posts/{id}")
    public Optional<Post> getPost(@PathVariable Long id) {
        return posts.findById(id);
    }

    @PostMapping("/posts")
    public Post writePost(@RequestBody PostRequest request) {
        return posts.save(new Post(request.memberId(), request.title(), request.content(),
                request.category() == null ? "자유" : request.category()));
    }

    @GetMapping("/posts/{postId}/comments")
    public List<Comment> listComments(@PathVariable Long postId) {
        return comments.findByPostIdOrderByCreatedAtAsc(postId);
    }

    @PostMapping("/posts/{postId}/comments")
    public Comment writeComment(@PathVariable Long postId, @RequestBody CommentRequest request) {
        return comments.save(new Comment(postId, request.memberId(), request.content()));
    }

    public record PostRequest(Long memberId, String title, String content, String category) {}

    public record CommentRequest(Long memberId, String content) {}
}
