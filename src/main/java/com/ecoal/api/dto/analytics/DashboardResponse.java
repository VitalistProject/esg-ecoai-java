package com.ecoal.api.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DashboardResponse {

    private List<DepartmentEntry> departments;
    private List<ContributionEntry> contributions;
    private List<EnergyEntry> energy;
    private List<TrainingEntry> training;

    public DashboardResponse() {
    }

    public DashboardResponse(
            List<DepartmentEntry> departments,
            List<ContributionEntry> contributions,
            List<EnergyEntry> energy,
            List<TrainingEntry> training) {
        this.departments = departments;
        this.contributions = contributions;
        this.energy = energy;
        this.training = training;
    }

    public List<DepartmentEntry> getDepartments() {
        return departments;
    }

    public void setDepartments(List<DepartmentEntry> departments) {
        this.departments = departments;
    }

    public List<ContributionEntry> getContributions() {
        return contributions;
    }

    public void setContributions(List<ContributionEntry> contributions) {
        this.contributions = contributions;
    }

    public List<EnergyEntry> getEnergy() {
        return energy;
    }

    public void setEnergy(List<EnergyEntry> energy) {
        this.energy = energy;
    }

    public List<TrainingEntry> getTraining() {
        return training;
    }

    public void setTraining(List<TrainingEntry> training) {
        this.training = training;
    }

    // -------------------------------------------------------------------------

    public static class DepartmentEntry {

        private String name;

        @JsonProperty("user_count")
        private long userCount;

        public DepartmentEntry() {
        }

        public DepartmentEntry(String name, long userCount) {
            this.name = name;
            this.userCount = userCount;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getUserCount() {
            return userCount;
        }

        public void setUserCount(long userCount) {
            this.userCount = userCount;
        }
    }

    // -------------------------------------------------------------------------

    public static class ContributionEntry {

        private String category;

        @JsonProperty("total_points")
        private double totalPoints;

        public ContributionEntry() {
        }

        public ContributionEntry(String category, double totalPoints) {
            this.category = category;
            this.totalPoints = totalPoints;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public double getTotalPoints() {
            return totalPoints;
        }

        public void setTotalPoints(double totalPoints) {
            this.totalPoints = totalPoints;
        }
    }

    // -------------------------------------------------------------------------

    public static class EnergyEntry {

        private String month;
        private int year;

        @JsonProperty("realized_value")
        private double realizedValue;

        @JsonProperty("goal_value")
        private double goalValue;

        public EnergyEntry() {
        }

        public EnergyEntry(String month, int year, double realizedValue, double goalValue) {
            this.month = month;
            this.year = year;
            this.realizedValue = realizedValue;
            this.goalValue = goalValue;
        }

        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public double getRealizedValue() {
            return realizedValue;
        }

        public void setRealizedValue(double realizedValue) {
            this.realizedValue = realizedValue;
        }

        public double getGoalValue() {
            return goalValue;
        }

        public void setGoalValue(double goalValue) {
            this.goalValue = goalValue;
        }
    }

    // -------------------------------------------------------------------------

    public static class TrainingEntry {

        private String quarter;
        private int year;

        @JsonProperty("total_hours")
        private double totalHours;

        public TrainingEntry() {
        }

        public TrainingEntry(String quarter, int year, double totalHours) {
            this.quarter = quarter;
            this.year = year;
            this.totalHours = totalHours;
        }

        public String getQuarter() {
            return quarter;
        }

        public void setQuarter(String quarter) {
            this.quarter = quarter;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }

        public double getTotalHours() {
            return totalHours;
        }

        public void setTotalHours(double totalHours) {
            this.totalHours = totalHours;
        }
    }
}
