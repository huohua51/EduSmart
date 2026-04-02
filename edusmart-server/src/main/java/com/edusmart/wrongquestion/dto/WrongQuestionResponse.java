package com.edusmart.wrongquestion.dto;

import java.time.Instant;
import java.util.List;

public class WrongQuestionResponse {

    private Long id;
    private String questionText;
    private String answer;
    private String analysis;
    private List<String> steps;
    private List<String> knowledgePoints;
    private String userAnswer;
    private String wrongReason;
    private int reviewCount;
    private Long lastReviewTime;
    private Long nextReviewTime;
    private Instant createdAt;
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }

    public List<String> getKnowledgePoints() { return knowledgePoints; }
    public void setKnowledgePoints(List<String> knowledgePoints) { this.knowledgePoints = knowledgePoints; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public String getWrongReason() { return wrongReason; }
    public void setWrongReason(String wrongReason) { this.wrongReason = wrongReason; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public Long getLastReviewTime() { return lastReviewTime; }
    public void setLastReviewTime(Long lastReviewTime) { this.lastReviewTime = lastReviewTime; }

    public Long getNextReviewTime() { return nextReviewTime; }
    public void setNextReviewTime(Long nextReviewTime) { this.nextReviewTime = nextReviewTime; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
