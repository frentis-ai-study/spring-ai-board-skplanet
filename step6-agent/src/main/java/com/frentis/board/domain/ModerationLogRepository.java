package com.frentis.board.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModerationLogRepository extends JpaRepository<ModerationLog, Long> {

    List<ModerationLog> findByActionOrderByCreatedAtDesc(String action);

    List<ModerationLog> findByMemberIdOrderByCreatedAtDesc(Long memberId);
}
