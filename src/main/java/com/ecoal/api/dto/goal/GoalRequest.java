package com.ecoal.api.dto.goal;

import com.ecoal.api.entity.Dimension;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GoalRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Dimension dimension;
}
