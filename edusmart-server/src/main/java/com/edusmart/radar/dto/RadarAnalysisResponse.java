package com.edusmart.radar.dto;

import java.util.List;

public class RadarAnalysisResponse {

    private int totalWrongQuestions;
    private List<KnowledgePointStat> knowledgePoints;
    private List<String> weakPoints;

    public int getTotalWrongQuestions() { return totalWrongQuestions; }
    public void setTotalWrongQuestions(int totalWrongQuestions) { this.totalWrongQuestions = totalWrongQuestions; }

    public List<KnowledgePointStat> getKnowledgePoints() { return knowledgePoints; }
    public void setKnowledgePoints(List<KnowledgePointStat> knowledgePoints) { this.knowledgePoints = knowledgePoints; }

    public List<String> getWeakPoints() { return weakPoints; }
    public void setWeakPoints(List<String> weakPoints) { this.weakPoints = weakPoints; }
}
