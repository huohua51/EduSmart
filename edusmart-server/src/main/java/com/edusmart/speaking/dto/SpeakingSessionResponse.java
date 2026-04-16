package com.edusmart.speaking.dto;

import java.time.Instant;
import java.util.List;

public class SpeakingSessionResponse {

    private Long id;
    private String learningPurpose;
    private String scene;
    private String customTopic;
    private Float totalScore;
    private int messageCount;
    private Long duration;
    private Instant createdAt;
    private Instant updatedAt;
    private List<SpeakingMessageResponse> messages;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLearningPurpose() {
        return learningPurpose;
    }

    public void setLearningPurpose(String learningPurpose) {
        this.learningPurpose = learningPurpose;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public String getCustomTopic() {
        return customTopic;
    }

    public void setCustomTopic(String customTopic) {
        this.customTopic = customTopic;
    }

    public Float getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Float totalScore) {
        this.totalScore = totalScore;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<SpeakingMessageResponse> getMessages() {
        return messages;
    }

    public void setMessages(List<SpeakingMessageResponse> messages) {
        this.messages = messages;
    }
}
