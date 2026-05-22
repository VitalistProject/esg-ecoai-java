package com.ecoal.api.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private List<DepartmentEntry> departments;
    private List<ContributionEntry> contributions;
    private List<EnergyEntry> energy;
    private List<TrainingEntry> training;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentEntry {

        private String name;

        @JsonProperty("user_count")
        private long userCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContributionEntry {

        private String category;

        @JsonProperty("total_points")
        private double totalPoints;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnergyEntry {

        private String month;
        private int year;

        @JsonProperty("realized_value")
        private double realizedValue;

        @JsonProperty("goal_value")
        private double goalValue;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrainingEntry {

        private String quarter;
        private int year;

        @JsonProperty("total_hours")
        private double totalHours;
    }
}
