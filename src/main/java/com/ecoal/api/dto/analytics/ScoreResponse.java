package com.ecoal.api.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreResponse {

    private double score;

    @JsonProperty("global_goal")
    private double globalGoal;

    private double percentage;
}
