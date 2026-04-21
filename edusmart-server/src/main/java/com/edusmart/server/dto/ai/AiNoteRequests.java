package com.edusmart.server.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * AI 笔记相关请求体集合。
 */
public final class AiNoteRequests {

    private AiNoteRequests() {}

    @Data
    public static class ContentRequest {
        @NotBlank(message = "content 不能为空")
        private String content;
        private String subject;
    }

    @Data
    public static class SubjectRequest {
        @NotBlank(message = "content 不能为空")
        private String content;
        private String title;
    }

    @Data
    public static class QaRequest {
        @NotBlank(message = "content 不能为空")
        private String content;
        @NotBlank(message = "question 不能为空")
        private String question;
    }

    @Data
    public static class TextResponse {
        private String text;
        public TextResponse() {}
        public TextResponse(String text) { this.text = text; }
    }

    @Data
    public static class TitleResponse {
        private String title;
        public TitleResponse() {}
        public TitleResponse(String title) { this.title = title; }
    }

    @Data
    public static class SubjectResponse {
        private String subject;
        public SubjectResponse() {}
        public SubjectResponse(String subject) { this.subject = subject; }
    }

    @Data
    public static class AnswerResponse {
        private String answer;
        public AnswerResponse() {}
        public AnswerResponse(String answer) { this.answer = answer; }
    }

    @Data
    public static class KnowledgePointsResponse {
        private List<String> points;
        public KnowledgePointsResponse() {}
        public KnowledgePointsResponse(List<String> points) { this.points = points; }
    }

    @Data
    public static class SummaryResponse {
        private String summary;
        private List<String> keyPoints;
        private List<String> tags;
        public SummaryResponse() {}
        public SummaryResponse(String summary, List<String> keyPoints, List<String> tags) {
            this.summary = summary;
            this.keyPoints = keyPoints;
            this.tags = tags;
        }
    }
}
