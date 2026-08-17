package com.frentis.board.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByCategoryOrderByCreatedAtDesc(String category);

    List<Post> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<Post> findByStatus(String status);

    @Query("""
            select p from Post p
            where lower(p.title) like lower(concat('%', :keyword, '%'))
               or lower(p.content) like lower(concat('%', :keyword, '%'))
            order by p.createdAt desc
            """)
    List<Post> searchByKeyword(@Param("keyword") String keyword);

    @Query("""
            select p from Post p
            where p.createdAt >= :from and p.status = 'PUBLISHED'
            order by p.viewCount desc
            """)
    List<Post> findPopularSince(@Param("from") LocalDateTime from);
}
