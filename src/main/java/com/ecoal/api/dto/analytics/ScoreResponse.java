package com.ecoal.api.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ScoreResponse {

    private double score;

    @JsonProperty("global_goal")
    private double globalGoal;

    private double percentage;

    public ScoreResponse() {
    }

    public ScoreResponse(double score, double globalGoal, double percentage) {
        this.score = score;
        this.globalGoal = globalGoal;
        this.percentage = percentage;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public double getGlobalGoal() {
        return globalGoal;
    }

    public void setGlobalGoal(double globalGoal) {
        this.globalGoal = globalGoal;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}
