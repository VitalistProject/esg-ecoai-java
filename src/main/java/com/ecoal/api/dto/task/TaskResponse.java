package com.ecoal.api.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class TaskResponse {

    private Long id;
    private String title;
    private String description;
    private Double score;
    private Boolean completed;

    @JsonProperty("completed_at")
    private OffsetDateTime completedAt;

    @JsonProperty("goal_id")
    private Long goalId;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}
