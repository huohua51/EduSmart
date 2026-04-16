package com.edusmart.speaking.dto;

public class SpeakingMessageRequest {

    private String role;
    private String content;
    private String translation;
    private Float score;
    private String suggestedReply;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }

    public Float getScore() {
        return score;
    }

    public void setScore(Float score) {
        this.score = score;
    }

    public String getSuggestedReply() {
        return suggestedReply;
    }

    public void setSuggestedReply(String suggestedReply) {
        this.suggestedReply = suggestedReply;
    }
}
