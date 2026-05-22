package com.ecoal.api.dto.goal;

import com.ecoal.api.dto.task.TaskResponse;
import com.ecoal.api.entity.Dimension;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class GoalResponse {

    private Long id;
    private String title;
    private String description;
    private Dimension dimension;

    @JsonProperty("company_id")
    private Long companyId;

    private List<TaskResponse> tasks;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}
