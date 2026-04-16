package com.edusmart.speaking.dto;

public class SpeakingSessionRequest {

    private String learningPurpose;
    private String scene;
    private String customTopic;
    private Long duration;

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

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }
}
