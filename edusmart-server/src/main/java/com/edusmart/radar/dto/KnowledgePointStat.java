package com.edusmart.radar.dto;

public class KnowledgePointStat {

    private String name;
    private int wrongCount;
    private double mastery;

    public KnowledgePointStat(String name, int wrongCount, double mastery) {
        this.name = name;
        this.wrongCount = wrongCount;
        this.mastery = mastery;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getWrongCount() { return wrongCount; }
    public void setWrongCount(int wrongCount) { this.wrongCount = wrongCount; }

    public double getMastery() { return mastery; }
    public void setMastery(double mastery) { this.mastery = mastery; }
}
