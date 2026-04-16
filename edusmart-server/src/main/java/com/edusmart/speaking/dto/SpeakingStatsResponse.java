package com.edusmart.speaking.dto;

import java.util.List;

public class SpeakingStatsResponse {

    private long totalSessions;
    private long totalDuration;
    private float averageScore;
    private List<Float> recentScores;
    private String favoriteScene;

    public long getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(long totalSessions) {
        this.totalSessions = totalSessions;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(long totalDuration) {
        this.totalDuration = totalDuration;
    }

    public float getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(float averageScore) {
        this.averageScore = averageScore;
    }

    public List<Float> getRecentScores() {
        return recentScores;
    }

    public void setRecentScores(List<Float> recentScores) {
        this.recentScores = recentScores;
    }

    public String getFavoriteScene() {
        return favoriteScene;
    }

    public void setFavoriteScene(String favoriteScene) {
        this.favoriteScene = favoriteScene;
    }
}
