package com.ecoal.api.controller;

import com.ecoal.api.entity.Company;
import com.ecoal.api.entity.Dimension;
import com.ecoal.api.entity.Goal;
import com.ecoal.api.entity.UserAccount;
import com.ecoal.api.repository.GoalRepository;
import com.ecoal.api.repository.TaskRepository;
import com.ecoal.api.support.ApiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GoalApiTest extends ApiTest {

    private static final int GOALS_PER_DIMENSION_LIMIT = 10;

    @Autowired
    private GoalRepository goalRepository;
    @Autowired
    private TaskRepository taskRepository;

    private Company company;
    private UserAccount user;

    @BeforeEach
    void createCompanyAndUser() {
        company = fixtures.company();
        user = fixtures.user(company);
    }

    // --- create ---

    @Test
    void createStoresGoalForTheCallersCompany() throws Exception {
        create("Cut emissions", "environmental")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Cut emissions"))
                .andExpect(jsonPath("$.dimension").value("environmental"))
                .andExpect(jsonPath("$.company_id").value(company.getId().intValue()))
                .andExpect(jsonPath("$.tasks").doesNotExist());

        assertThat(goalRepository.countByCompanyIdAndDimension(company.getId(), Dimension.ENVIRONMENTAL)).isEqualTo(1);
    }

    @Test
    void createRejectsBlankTitleAndMissingDimension() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .header(AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists())
                .andExpect(jsonPath("$.fields.dimension").exists());
    }

    @Test
    void createAcceptsTheTenthGoalOfADimension() throws Exception {
        givenGoals(GOALS_PER_DIMENSION_LIMIT - 1, Dimension.SOCIAL);

        create("Tenth", "social").andExpect(status().isCreated());
    }

    @Test
    void createRejectsTheEleventhGoalOfADimension() throws Exception {
        givenGoals(GOALS_PER_DIMENSION_LIMIT, Dimension.SOCIAL);

        create("Eleventh", "social")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Maximum 10 goals per dimension"));

        assertThat(goalRepository.countByCompanyIdAndDimension(company.getId(), Dimension.SOCIAL))
                .isEqualTo(GOALS_PER_DIMENSION_LIMIT);
    }

    @Test
    void goalLimitIsPerDimension() throws Exception {
        givenGoals(GOALS_PER_DIMENSION_LIMIT, Dimension.SOCIAL);

        create("Different dimension", "governance").andExpect(status().isCreated());
    }

    @Test
    void goalLimitIsPerCompany() throws Exception {
        Company other = fixtures.company();
        IntStream.range(0, GOALS_PER_DIMENSION_LIMIT).forEach(i -> fixtures.goal(other, Dimension.SOCIAL));

        create("Mine", "social").andExpect(status().isCreated());
    }

    // --- list ---

    @Test
    void listReturnsOnlyTheCallersCompanyGoalsNewestFirst() throws Exception {
        Goal oldest = fixtures.goal(company, Dimension.SOCIAL);
        Goal newest = fixtures.goal(company, Dimension.GOVERNANCE);
        Goal middle = fixtures.goal(company, Dimension.ENVIRONMENTAL);
        fixtures.goal(fixtures.company(), Dimension.SOCIAL);
        fixtures.backdateGoal(oldest, 30);
        fixtures.backdateGoal(middle, 10);
        fixtures.backdateGoal(newest, 1);

        mockMvc.perform(get("/api/goals").header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(
                        newest.getId().intValue(), middle.getId().intValue(), oldest.getId().intValue())))
                .andExpect(jsonPath("$[0].tasks").value(nullValue()));
    }

    @Test
    void listFiltersByDimension() throws Exception {
        Goal social = fixtures.goal(company, Dimension.SOCIAL);
        fixtures.goal(company, Dimension.GOVERNANCE);

        mockMvc.perform(get("/api/goals").param("dimension", "SOCIAL").header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(social.getId().intValue()));
    }

    @Test
    void listAcceptsDimensionInItsLowercaseJsonRepresentation() throws Exception {
        Goal social = fixtures.goal(company, Dimension.SOCIAL);
        fixtures.goal(company, Dimension.GOVERNANCE);

        mockMvc.perform(get("/api/goals").param("dimension", "social").header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(social.getId().intValue())));
    }

    @Test
    void listTreatsBlankDimensionAsNoFilter() throws Exception {
        fixtures.goal(company, Dimension.SOCIAL);
        fixtures.goal(company, Dimension.GOVERNANCE);

        mockMvc.perform(get("/api/goals").param("dimension", "").header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void listFiltersByCreationDateInclusiveOfThatDay() throws Exception {
        Goal old = fixtures.goal(company, Dimension.SOCIAL);
        Goal recent = fixtures.goal(company, Dimension.SOCIAL);
        fixtures.backdateGoal(old, 10);
        fixtures.backdateGoal(recent, 2);

        mockMvc.perform(get("/api/goals").param("created_at", daysAgoIsoDate(5)).header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(recent.getId().intValue())));
    }

    @Test
    void listCombinesDimensionAndCreationDateFilters() throws Exception {
        Goal oldSocial = fixtures.goal(company, Dimension.SOCIAL);
        Goal recentSocial = fixtures.goal(company, Dimension.SOCIAL);
        Goal recentGovernance = fixtures.goal(company, Dimension.GOVERNANCE);
        fixtures.backdateGoal(oldSocial, 10);
        fixtures.backdateGoal(recentSocial, 2);
        fixtures.backdateGoal(recentGovernance, 2);

        mockMvc.perform(get("/api/goals")
                        .param("dimension", "SOCIAL")
                        .param("created_at", daysAgoIsoDate(5))
                        .header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(recentSocial.getId().intValue())));
    }

    @Test
    void listIsEmptyForCompanyWithoutGoals() throws Exception {
        mockMvc.perform(get("/api/goals").header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // --- get ---

    @Test
    void getReturnsGoalWithItsTasks() throws Exception {
        Goal goal = fixtures.goal(company, Dimension.ENVIRONMENTAL);
        fixtures.task(goal, 10.0, true);
        fixtures.task(goal, 5.0, false);

        mockMvc.perform(get("/api/goals/{id}", goal.getId()).header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(goal.getId().intValue()))
                .andExpect(jsonPath("$.tasks", hasSize(2)))
                .andExpect(jsonPath("$.tasks[0].goal_id").value(goal.getId().intValue()));
    }

    @Test
    void getHidesGoalOfAnotherCompany() throws Exception {
        Goal foreign = fixtures.goal(fixtures.company(), Dimension.SOCIAL);

        mockMvc.perform(get("/api/goals/{id}", foreign.getId()).header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Goal not found"));
    }

    // --- update ---

    @Test
    void updateChangesTitleDescriptionAndDimension() throws Exception {
        Goal goal = fixtures.goal(company, Dimension.SOCIAL);

        mockMvc.perform(put("/api/goals/{id}", goal.getId())
                        .header(AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(goalBody("Renamed", "governance")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Renamed"))
                .andExpect(jsonPath("$.dimension").value("governance"));

        Goal stored = goalRepository.findById(goal.getId()).orElseThrow();
        assertThat(stored.getTitle()).isEqualTo("Renamed");
        assertThat(stored.getDimension()).isEqualTo(Dimension.GOVERNANCE);
    }

    @Test
    void updateRejectsMovingIntoADimensionAlreadyAtTheLimit() throws Exception {
        givenGoals(GOALS_PER_DIMENSION_LIMIT, Dimension.SOCIAL);
        Goal goal = fixtures.goal(company, Dimension.GOVERNANCE);

        update(goal, "Moved", "social")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Maximum 10 goals per dimension"));

        assertThat(goalRepository.findById(goal.getId()).orElseThrow().getDimension()).isEqualTo(Dimension.GOVERNANCE);
    }

    @Test
    void updateKeepsWorkingForGoalInsideAFullDimension() throws Exception {
        givenGoals(GOALS_PER_DIMENSION_LIMIT - 1, Dimension.SOCIAL);
        Goal goal = fixtures.goal(company, Dimension.SOCIAL);

        update(goal, "Still social", "social").andExpect(status().isOk());
    }

    @Test
    void updateHidesGoalOfAnotherCompany() throws Exception {
        Goal foreign = fixtures.goal(fixtures.company(), Dimension.SOCIAL);

        update(foreign, "Hijacked", "social").andExpect(status().isNotFound());

        assertThat(goalRepository.findById(foreign.getId()).orElseThrow().getTitle())
                .isEqualTo(foreign.getTitle());
    }

    // --- delete ---

    @Test
    void deleteRemovesGoalAndItsTasks() throws Exception {
        Goal goal = fixtures.goal(company, Dimension.SOCIAL);
        fixtures.task(goal, 10.0, false);
        fixtures.task(goal, 10.0, true);

        mockMvc.perform(delete("/api/goals/{id}", goal.getId()).header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isNoContent());

        assertThat(goalRepository.existsById(goal.getId())).isFalse();
        assertThat(taskRepository.countByGoalId(goal.getId())).isZero();
    }

    @Test
    void deleteHidesGoalOfAnotherCompanyAndKeepsIt() throws Exception {
        Goal foreign = fixtures.goal(fixtures.company(), Dimension.SOCIAL);

        mockMvc.perform(delete("/api/goals/{id}", foreign.getId()).header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isNotFound());

        assertThat(goalRepository.existsById(foreign.getId())).isTrue();
    }

    // --- helpers ---

    private ResultActions create(String title, String dimension) throws Exception {
        return mockMvc.perform(post("/api/goals")
                .header(AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(goalBody(title, dimension)));
    }

    private ResultActions update(Goal goal, String title, String dimension) throws Exception {
        return mockMvc.perform(put("/api/goals/{id}", goal.getId())
                .header(AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(goalBody(title, dimension)));
    }

    private void givenGoals(int count, Dimension dimension) {
        IntStream.range(0, count).forEach(i -> fixtures.goal(company, dimension));
    }

    private static String goalBody(String title, String dimension) {
        return """
                {"title": "%s", "description": "desc", "dimension": "%s"}""".formatted(title, dimension);
    }

    private static String daysAgoIsoDate(int days) {
        return LocalDate.now(ZoneOffset.UTC).minusDays(days).toString();
    }
}
