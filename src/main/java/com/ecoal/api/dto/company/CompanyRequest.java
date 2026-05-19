package com.ecoal.api.dto.company;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class CompanyRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String cnpj;

    @JsonProperty("global_score_goal")
    private Double globalScoreGoal;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public Double getGlobalScoreGoal() {
        return globalScoreGoal;
    }

    public void setGlobalScoreGoal(Double globalScoreGoal) {
        this.globalScoreGoal = globalScoreGoal;
    }
}
