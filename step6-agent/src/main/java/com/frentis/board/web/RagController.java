package com.frentis.board.web;

import com.frentis.board.rag.PolicyIndexer;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * step5: 운영정책 문서 인덱싱과 확인.
 *
 * 실습 확인용
 *   # 1) 정책 문서 임베딩 (최초 1회)
 *   curl -s -X POST localhost:8080/api/rag/index
 *   # 2) 문서 근거 답변 확인
 *   curl -s -X POST localhost:8080/api/chat -H 'Content-Type: application/json' \
 *        -d '{"conversationId":"u-1","message":"제 글이 갑자기 안 보이는데 왜 그런가요?"}'
 *   curl -s -X POST localhost:8080/api/chat -H 'Content-Type: application/json' \
 *        -d '{"conversationId":"u-1","message":"제재를 3번 받으면 어떻게 되나요?"}'
 *   # 3) 정책에 없는 질문에는 모른다고 답하는지 확인
 *   curl -s -X POST localhost:8080/api/chat -H 'Content-Type: application/json' \
 *        -d '{"conversationId":"u-1","message":"포인트는 언제 지급되나요?"}'
 */
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final PolicyIndexer indexer;

    public RagController(PolicyIndexer indexer) {
        this.indexer = indexer;
    }

    @PostMapping("/index")
    public Map<String, Object> index() {
        int chunks = indexer.indexAll();
        return Map.of("indexedChunks", chunks, "message", "운영정책 문서 인덱싱을 마쳤습니다.");
    }
}
