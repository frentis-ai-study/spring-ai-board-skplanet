package com.frentis.board.advisor;

import java.text.Normalizer;

/**
 * 금지어 우회를 줄이기 위한 사전 정규화.
 *
 * 한국어는 자모 분리(ㅇㅕㅇ), 반복 문자(좋아아아아), 특수문자 삽입(욕*설) 같은 우회가 흔합니다.
 * 정규화 없이 단순 문자열 포함 검사만 하면 대부분 뚫립니다.
 */
public final class TextNormalizer {

    private TextNormalizer() {}

    public static String normalize(String text) {
        if (text == null) return "";

        // 1) 자모 결합 (NFC) — ㅎㅏㄴ 형태로 쪼갠 우회를 되돌린다
        String result = Normalizer.normalize(text, Normalizer.Form.NFC);

        // 2) 소문자화
        result = result.toLowerCase();

        // 3) 한글·영문·숫자만 남기고 제거 — 욕*설, 욕.설 같은 삽입 우회 차단
        result = result.replaceAll("[^가-힣a-z0-9]", "");

        // 4) 3회 이상 반복되는 문자를 1회로 축약 — 좋아아아아 → 좋아
        result = result.replaceAll("(.)\\1{2,}", "$1");

        return result;
    }
}
