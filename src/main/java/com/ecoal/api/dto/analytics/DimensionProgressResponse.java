package com.ecoal.api.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DimensionProgressResponse {

    private List<DimensionEntry> dimensions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DimensionEntry {

        private String dimension;
        private int completed;
        private int total;
        private double percentage;
    }
}
