package com.ecoal.api.dto.company;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompanyRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String cnpj;

    @JsonProperty("global_score_goal")
    private Double globalScoreGoal;
}
