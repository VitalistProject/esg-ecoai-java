package com.ecoal.api.dto.company;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CompanyResponse {

    private Long id;
    private String name;
    private String cnpj;

    @JsonProperty("global_score_goal")
    private Double globalScoreGoal;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;
}
