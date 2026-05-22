package com.ecoal.api.dto.task;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskRequest {

    @NotBlank
    private String title;

    private String description;
    private Double score;
    private Boolean completed;
}
