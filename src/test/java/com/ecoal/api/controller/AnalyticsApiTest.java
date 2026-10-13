package com.ecoal.api.controller;

import com.ecoal.api.entity.Company;
import com.ecoal.api.entity.Dimension;
import com.ecoal.api.entity.Goal;
import com.ecoal.api.entity.UserAccount;
import com.ecoal.api.support.ApiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalyticsApiTest extends ApiTest {

    private Company company;
    private UserAccount user;

    @BeforeEach
    void createCompanyAndUser() {
        company = fixtures.company(200.0);
        user = fixtures.user(company, "admin", "Engineering");
    }

    // --- score ---

    @Test
    void scoreSumsOnlyCompletedTasksAcrossAllGoals() throws Exception {
        Goal environmental = fixtures.goal(company, Dimension.ENVIRONMENTAL);
        Goal social = fixtures.goal(company, Dimension.SOCIAL);
        fixtures.task(environmental, 30.0, true);
        fixtures.task(social, 20.0, true);
        fixtures.task(environmental, 100.0, false);

        score()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(50.0))
                .andExpect(jsonPath("$.global_goal").value(200.0))
                .andExpect(jsonPath("$.percentage").value(25.0));
    }

    @Test
    void scoreIgnoresCompletedTasksWithoutScore() throws Exception {
        Goal goal = fixtures.goal(company, Dimension.SOCIAL);
        fixtures.task(goal, null, true);
        fixtures.task(goal, 10.0, true);

        score().andExpect(jsonPath("$.score").value(10.0));
    }

    @Test
    void scoreIgnoresOtherCompanies() throws Exception {
        fixtures.task(fixtures.goal(fixtures.company(), Dimension.SOCIAL), 80.0, true);

        score().andExpect(jsonPath("$.score").value(0.0));
    }

    @Test
    void scoreCanExceedTheGlobalGoal() throws Exception {
        fixtures.task(fixtures.goal(company, Dimension.SOCIAL), 300.0, true);

        score().andExpect(jsonPath("$.percentage").value(150.0));
    }

    @Test
    void scorePercentageIsZeroWhenGlobalGoalIsZero() throws Exception {
        Company zeroGoal = fixtures.company(0.0);
        UserAccount zeroGoalUser = fixtures.user(zeroGoal);
        fixtures.task(fixtures.goal(zeroGoal, Dimension.SOCIAL), 10.0, true);

        mockMvc.perform(get("/api/analytics/score").header(AUTHORIZATION, bearer(zeroGoalUser)))
                .andExpect(jsonPath("$.score").value(10.0))
                .andExpect(jsonPath("$.global_goal").value(0.0))
                .andExpect(jsonPath("$.percentage").value(0.0));
    }

    @Test
    void scoreTreatsMissingGlobalGoalAsZero() throws Exception {
        Company noGoal = fixtures.company(null);
        UserAccount noGoalUser = fixtures.user(noGoal);

        mockMvc.perform(get("/api/analytics/score").header(AUTHORIZATION, bearer(noGoalUser)))
                .andExpect(jsonPath("$.global_goal").value(0.0))
                .andExpect(jsonPath("$.percentage").value(0.0));
    }

    // --- dimensions ---

    @Test
    void dimensionsReportsEveryDimensionInFixedOrder() throws Exception {
        dimensions(null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimensions[*].dimension", contains("environmental", "social", "governance")))
                .andExpect(jsonPath("$.dimensions[*].total", contains(0, 0, 0)))
                .andExpect(jsonPath("$.dimensions[*].percentage", contains(0.0, 0.0, 0.0)));
    }

    @Test
    void dimensionsCountsCompletedAndTotalTasksPerDimension() throws Exception {
        Goal environmental = fixtures.goal(company, Dimension.ENVIRONMENTAL);
        fixtures.task(environmental, 1.0, true);
        fixtures.task(environmental, 1.0, false);
        Goal governance = fixtures.goal(company, Dimension.GOVERNANCE);
        fixtures.task(governance, 1.0, true);
        fixtures.task(governance, 1.0, false);
        fixtures.task(governance, 1.0, false);

        dimensions(null)
                .andExpect(jsonPath("$.dimensions[0].completed").value(1))
                .andExpect(jsonPath("$.dimensions[0].total").value(2))
                .andExpect(jsonPath("$.dimensions[0].percentage").value(50.0))
                .andExpect(jsonPath("$.dimensions[1].total").value(0))
                .andExpect(jsonPath("$.dimensions[2].completed").value(1))
                .andExpect(jsonPath("$.dimensions[2].total").value(3))
                .andExpect(jsonPath("$.dimensions[2].percentage").value(closeTo(33.33, 0.01)));
    }

    @Test
    void dimensionsAggregatesTasksOfSeveralGoalsInTheSameDimension() throws Exception {
        fixtures.task(fixtures.goal(company, Dimension.SOCIAL), 1.0, true);
        fixtures.task(fixtures.goal(company, Dimension.SOCIAL), 1.0, false);

        dimensions(null)
                .andExpect(jsonPath("$.dimensions[1].completed").value(1))
                .andExpect(jsonPath("$.dimensions[1].total").value(2));
    }

    @Test
    void dimensionsIgnoreOtherCompanies() throws Exception {
        fixtures.task(fixtures.goal(fixtures.company(), Dimension.SOCIAL), 1.0, true);

        dimensions(null).andExpect(jsonPath("$.dimensions[1].total").value(0));
    }

    @Test
    void dimensionsDefaultToTheLastMonth() throws Exception {
        givenSocialGoalWithOneTaskCreated(45);
        Goal recent = givenSocialGoalWithOneTaskCreated(10);
        fixtures.task(recent, 1.0, true);

        dimensions(null)
                .andExpect(jsonPath("$.dimensions[1].total").value(2))
                .andExpect(jsonPath("$.dimensions[1].completed").value(1));
    }

    @Test
    void dimensionsWithUnknownPeriodFallBackToTheLastMonth() throws Exception {
        givenSocialGoalWithOneTaskCreated(45);
        givenSocialGoalWithOneTaskCreated(10);

        dimensions("fortnightly").andExpect(jsonPath("$.dimensions[1].total").value(1));
    }

    @Test
    void dimensionsQuarterlyWindowCoversThreeMonths() throws Exception {
        givenSocialGoalWithOneTaskCreated(120);
        givenSocialGoalWithOneTaskCreated(60);

        dimensions("quarterly").andExpect(jsonPath("$.dimensions[1].total").value(1));
    }

    @Test
    void dimensionsAnnualWindowCoversTwelveMonths() throws Exception {
        givenSocialGoalWithOneTaskCreated(400);
        givenSocialGoalWithOneTaskCreated(200);

        dimensions("annual").andExpect(jsonPath("$.dimensions[1].total").value(1));
    }

    // --- dashboard ---

    @Test
    void dashboardIsEmptyForCompanyWithoutMetrics() throws Exception {
        Company bare = fixtures.company();
        UserAccount bareUser = fixtures.user(bare);

        mockMvc.perform(get("/api/analytics/dashboard").header(AUTHORIZATION, bearer(bareUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contributions", empty()))
                .andExpect(jsonPath("$.energy", empty()))
                .andExpect(jsonPath("$.training", empty()));
    }

    @Test
    void dashboardCountsCompanyUsersPerDepartment() throws Exception {
        fixtures.user(company, "user", "Engineering");
        fixtures.user(company, "user", "Finance");
        fixtures.user(fixtures.company(), "user", "Engineering");

        dashboard()
                .andExpect(jsonPath("$.departments[*].name", containsInAnyOrder("Engineering", "Finance")))
                .andExpect(jsonPath("$.departments[?(@.name=='Engineering')].user_count", contains(2)))
                .andExpect(jsonPath("$.departments[?(@.name=='Finance')].user_count", contains(1)));
    }

    @Test
    void dashboardSumsContributionPointsPerCategoryForCompanyUsersOnly() throws Exception {
        UserAccount colleague = fixtures.user(company);
        fixtures.contribution(user, "volunteering", 50.0);
        fixtures.contribution(colleague, "volunteering", 25.5);
        fixtures.contribution(colleague, "training", 30.0);
        fixtures.contribution(fixtures.user(fixtures.company()), "volunteering", 1000.0);

        dashboard()
                .andExpect(jsonPath("$.contributions[*].category", containsInAnyOrder("volunteering", "training")))
                .andExpect(jsonPath("$.contributions[?(@.category=='volunteering')].total_points", contains(75.5)))
                .andExpect(jsonPath("$.contributions[?(@.category=='training')].total_points", contains(30.0)));
    }

    @Test
    void dashboardListsEnergyMetricsOfTheCompanyOnly() throws Exception {
        fixtures.energy(company, "March", 2026, 3900.0, 4000.0);
        fixtures.energy(fixtures.company(), "March", 2026, 1.0, 2.0);

        dashboard()
                .andExpect(jsonPath("$.energy[*].month", contains("March")))
                .andExpect(jsonPath("$.energy[0].year").value(2026))
                .andExpect(jsonPath("$.energy[0].realized_value").value(3900.0))
                .andExpect(jsonPath("$.energy[0].goal_value").value(4000.0));
    }

    @Test
    void dashboardOrdersEnergyMetricsByYear() throws Exception {
        fixtures.energy(company, "January", 2026, 1.0, 1.0);
        fixtures.energy(company, "December", 2025, 1.0, 1.0);

        dashboard().andExpect(jsonPath("$.energy[*].year", contains(2025, 2026)));
    }

    @Test
    void dashboardOrdersEnergyMetricsChronologicallyWithinAYear() throws Exception {
        fixtures.energy(company, "March", 2026, 1.0, 1.0);
        fixtures.energy(company, "January", 2026, 1.0, 1.0);
        fixtures.energy(company, "February", 2026, 1.0, 1.0);

        dashboard().andExpect(jsonPath("$.energy[*].month", contains("January", "February", "March")));
    }

    @Test
    void dashboardPutsEnergyMetricsWithUnrecognizedMonthAfterDecember() throws Exception {
        fixtures.energy(company, "Sprint", 2026, 1.0, 1.0);
        fixtures.energy(company, "December", 2026, 1.0, 1.0);
        fixtures.energy(company, "february", 2026, 1.0, 1.0);

        dashboard().andExpect(jsonPath("$.energy[*].month", contains("february", "December", "Sprint")));
    }

    @Test
    void dashboardListsTrainingMetricsByYearThenQuarter() throws Exception {
        fixtures.training(company, "Q2", 2026, 40.0);
        fixtures.training(company, "Q1", 2026, 120.0);
        fixtures.training(company, "Q4", 2025, 10.0);
        fixtures.training(fixtures.company(), "Q1", 2026, 999.0);

        dashboard()
                .andExpect(jsonPath("$.training[*].quarter", contains("Q4", "Q1", "Q2")))
                .andExpect(jsonPath("$.training[*].year", contains(2025, 2026, 2026)))
                .andExpect(jsonPath("$.training[*].total_hours", contains(10.0, 120.0, 40.0)));
    }

    // --- authentication ---

    @Test
    void analyticsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/analytics/score")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/analytics/dimensions")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/analytics/dashboard")).andExpect(status().isForbidden());
    }

    // --- helpers ---

    private ResultActions score() throws Exception {
        return mockMvc.perform(get("/api/analytics/score").header(AUTHORIZATION, bearer(user)));
    }

    private ResultActions dimensions(String period) throws Exception {
        var request = get("/api/analytics/dimensions").header(AUTHORIZATION, bearer(user));
        return mockMvc.perform(period == null ? request : request.param("period", period));
    }

    private ResultActions dashboard() throws Exception {
        return mockMvc.perform(get("/api/analytics/dashboard").header(AUTHORIZATION, bearer(user)));
    }

    private Goal givenSocialGoalWithOneTaskCreated(int daysAgo) {
        Goal goal = fixtures.goal(company, Dimension.SOCIAL);
        fixtures.task(goal, 1.0, false);
        fixtures.backdateGoal(goal, daysAgo);
        return goal;
    }
}
