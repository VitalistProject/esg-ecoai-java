package com.ecoal.api.dto.analytics;

import java.util.List;

public class DimensionProgressResponse {

    private List<DimensionEntry> dimensions;

    public DimensionProgressResponse() {
    }

    public DimensionProgressResponse(List<DimensionEntry> dimensions) {
        this.dimensions = dimensions;
    }

    public List<DimensionEntry> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<DimensionEntry> dimensions) {
        this.dimensions = dimensions;
    }

    public static class DimensionEntry {

        private String dimension;
        private int completed;
        private int total;
        private double percentage;

        public DimensionEntry() {
        }

        public DimensionEntry(String dimension, int completed, int total, double percentage) {
            this.dimension = dimension;
            this.completed = completed;
            this.total = total;
            this.percentage = percentage;
        }

        public String getDimension() {
            return dimension;
        }

        public void setDimension(String dimension) {
            this.dimension = dimension;
        }

        public int getCompleted() {
            return completed;
        }

        public void setCompleted(int completed) {
            this.completed = completed;
        }

        public int getTotal() {
            return total;
        }

        public void setTotal(int total) {
            this.total = total;
        }

        public double getPercentage() {
            return percentage;
        }

        public void setPercentage(double percentage) {
            this.percentage = percentage;
        }
    }
}
