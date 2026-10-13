package com.ecoal.api.controller;

import com.ecoal.api.entity.Company;
import com.ecoal.api.entity.Dimension;
import com.ecoal.api.entity.Goal;
import com.ecoal.api.entity.Task;
import com.ecoal.api.entity.UserAccount;
import com.ecoal.api.repository.TaskRepository;
import com.ecoal.api.support.ApiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.OffsetDateTime;
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

class TaskApiTest extends ApiTest {

    private static final int TASKS_PER_GOAL_LIMIT = 10;

    @Autowired
    private TaskRepository taskRepository;

    private UserAccount user;
    private Goal goal;

    @BeforeEach
    void createCompanyUserAndGoal() {
        Company company = fixtures.company();
        user = fixtures.user(company);
        goal = fixtures.goal(company, Dimension.ENVIRONMENTAL);
    }

    // --- create ---

    @Test
    void createStoresIncompleteTaskUnderTheGoal() throws Exception {
        create(goal, taskBody("Install panels", 15.5, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Install panels"))
                .andExpect(jsonPath("$.score").value(15.5))
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.completed_at").value(nullValue()))
                .andExpect(jsonPath("$.goal_id").value(goal.getId().intValue()));

        assertThat(taskRepository.countByGoalId(goal.getId())).isEqualTo(1);
    }

    @Test
    void createIgnoresCompletedFlagSentByTheClient() throws Exception {
        create(goal, taskBody("Pre-completed", 5.0, true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    void createRejectsBlankTitle() throws Exception {
        create(goal, taskBody("", 5.0, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists());
    }

    @Test
    void createAcceptsTheTenthTaskOfAGoal() throws Exception {
        givenTasks(goal, TASKS_PER_GOAL_LIMIT - 1);

        create(goal, taskBody("Tenth", 1.0, null)).andExpect(status().isCreated());
    }

    @Test
    void createRejectsTheEleventhTaskOfAGoal() throws Exception {
        givenTasks(goal, TASKS_PER_GOAL_LIMIT);

        create(goal, taskBody("Eleventh", 1.0, null))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Maximum 10 tasks per goal"));

        assertThat(taskRepository.countByGoalId(goal.getId())).isEqualTo(TASKS_PER_GOAL_LIMIT);
    }

    @Test
    void taskLimitIsPerGoal() throws Exception {
        givenTasks(goal, TASKS_PER_GOAL_LIMIT);
        Goal another = fixtures.goal(goal.getCompany(), Dimension.SOCIAL);

        create(another, taskBody("Fits here", 1.0, null)).andExpect(status().isCreated());
    }

    @Test
    void createHidesGoalOfAnotherCompany() throws Exception {
        Goal foreign = fixtures.goal(fixtures.company(), Dimension.SOCIAL);

        create(foreign, taskBody("Intruder", 1.0, null))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Goal not found"));

        assertThat(taskRepository.countByGoalId(foreign.getId())).isZero();
    }

    // --- list ---

    @Test
    void listReturnsTheGoalsTasksNewestFirst() throws Exception {
        Task oldest = fixtures.task(goal, 1.0, false);
        Task newest = fixtures.task(goal, 2.0, false);
        Task middle = fixtures.task(goal, 3.0, false);
        fixtures.task(fixtures.goal(goal.getCompany(), Dimension.SOCIAL), 4.0, false);
        fixtures.backdateTask(oldest, 30);
        fixtures.backdateTask(middle, 10);
        fixtures.backdateTask(newest, 1);

        list(goal)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(
                        newest.getId().intValue(), middle.getId().intValue(), oldest.getId().intValue())));
    }

    @Test
    void listFiltersByCompletion() throws Exception {
        Task done = fixtures.task(goal, 1.0, true);
        Task pending = fixtures.task(goal, 1.0, false);

        list(goal, "completed", "true").andExpect(jsonPath("$[*].id", contains(done.getId().intValue())));
        list(goal, "completed", "false").andExpect(jsonPath("$[*].id", contains(pending.getId().intValue())));
    }

    @Test
    void listFiltersByCreationDate() throws Exception {
        Task old = fixtures.task(goal, 1.0, false);
        Task recent = fixtures.task(goal, 1.0, false);
        fixtures.backdateTask(old, 10);
        fixtures.backdateTask(recent, 2);

        list(goal, "created_at", daysAgoIsoDate(5))
                .andExpect(jsonPath("$[*].id", contains(recent.getId().intValue())));
    }

    @Test
    void listCombinesCompletionAndCreationDateFilters() throws Exception {
        Task oldDone = fixtures.task(goal, 1.0, true);
        Task recentDone = fixtures.task(goal, 1.0, true);
        Task recentPending = fixtures.task(goal, 1.0, false);
        fixtures.backdateTask(oldDone, 10);
        fixtures.backdateTask(recentDone, 2);
        fixtures.backdateTask(recentPending, 2);

        mockMvc.perform(get("/api/goals/{goalId}/tasks", goal.getId())
                        .param("completed", "true")
                        .param("created_at", daysAgoIsoDate(5))
                        .header(AUTHORIZATION, bearer(user)))
                .andExpect(jsonPath("$[*].id", contains(recentDone.getId().intValue())));
    }

    @Test
    void listHidesGoalOfAnotherCompany() throws Exception {
        Goal foreign = fixtures.goal(fixtures.company(), Dimension.SOCIAL);
        fixtures.task(foreign, 1.0, false);

        list(foreign).andExpect(status().isNotFound());
    }

    // --- get ---

    @Test
    void getReturnsTheTask() throws Exception {
        Task task = fixtures.task(goal, 7.0, false);

        mockMvc.perform(get("/api/goals/{goalId}/tasks/{id}", goal.getId(), task.getId())
                        .header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId().intValue()))
                .andExpect(jsonPath("$.score").value(7.0));
    }

    @Test
    void getDoesNotFindATaskThroughAnotherGoal() throws Exception {
        Task task = fixtures.task(goal, 7.0, false);
        Goal sibling = fixtures.goal(goal.getCompany(), Dimension.SOCIAL);

        mockMvc.perform(get("/api/goals/{goalId}/tasks/{id}", sibling.getId(), task.getId())
                        .header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Task not found"));
    }

    // --- update ---

    @Test
    void updateChangesTitleDescriptionAndScore() throws Exception {
        Task task = fixtures.task(goal, 1.0, false);

        update(task, taskBody("Rewritten", 42.0, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Rewritten"))
                .andExpect(jsonPath("$.score").value(42.0));

        Task stored = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(stored.getTitle()).isEqualTo("Rewritten");
        assertThat(stored.getScore()).isEqualTo(42.0);
    }

    @Test
    void updateMarkingCompletedStampsCompletionTime() throws Exception {
        Task task = fixtures.task(goal, 1.0, false);
        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1);

        update(task, taskBody("Done", 1.0, true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.completed_at").isNotEmpty());

        OffsetDateTime completedAt = taskRepository.findById(task.getId()).orElseThrow().getCompletedAt();
        assertThat(completedAt).isBetween(before, OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(1));
    }

    @Test
    void updateKeepsOriginalCompletionTimeWhenAlreadyCompleted() throws Exception {
        Task task = fixtures.task(goal, 1.0, false);
        update(task, taskBody("Done", 1.0, true)).andExpect(status().isOk());
        OffsetDateTime firstCompletion = taskRepository.findById(task.getId()).orElseThrow().getCompletedAt();

        update(task, taskBody("Done again", 1.0, true)).andExpect(status().isOk());

        assertThat(taskRepository.findById(task.getId()).orElseThrow().getCompletedAt()).isEqualTo(firstCompletion);
    }

    @Test
    void updateReopeningClearsCompletionTime() throws Exception {
        Task task = fixtures.task(goal, 1.0, true);

        update(task, taskBody("Reopened", 1.0, false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(false))
                .andExpect(jsonPath("$.completed_at").value(nullValue()));

        Task stored = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(stored.getCompleted()).isFalse();
        assertThat(stored.getCompletedAt()).isNull();
    }

    @Test
    void updateWithoutCompletedFlagLeavesCompletionUntouched() throws Exception {
        Task task = fixtures.task(goal, 1.0, true);

        update(task, taskBody("Edited only", 2.0, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));

        assertThat(taskRepository.findById(task.getId()).orElseThrow().getCompletedAt()).isNotNull();
    }

    @Test
    void updateHidesTaskOfAnotherCompany() throws Exception {
        Goal foreignGoal = fixtures.goal(fixtures.company(), Dimension.SOCIAL);
        Task foreign = fixtures.task(foreignGoal, 1.0, false);

        mockMvc.perform(put("/api/goals/{goalId}/tasks/{id}", foreignGoal.getId(), foreign.getId())
                        .header(AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(taskBody("Hijacked", 99.0, true)))
                .andExpect(status().isNotFound());

        assertThat(taskRepository.findById(foreign.getId()).orElseThrow().getTitle()).isEqualTo(foreign.getTitle());
    }

    // --- delete ---

    @Test
    void deleteRemovesTheTask() throws Exception {
        Task task = fixtures.task(goal, 1.0, false);

        mockMvc.perform(delete("/api/goals/{goalId}/tasks/{id}", goal.getId(), task.getId())
                        .header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isNoContent());

        assertThat(taskRepository.existsById(task.getId())).isFalse();
    }

    @Test
    void deleteHidesTaskOfAnotherCompanyAndKeepsIt() throws Exception {
        Goal foreignGoal = fixtures.goal(fixtures.company(), Dimension.SOCIAL);
        Task foreign = fixtures.task(foreignGoal, 1.0, false);

        mockMvc.perform(delete("/api/goals/{goalId}/tasks/{id}", foreignGoal.getId(), foreign.getId())
                        .header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isNotFound());

        assertThat(taskRepository.existsById(foreign.getId())).isTrue();
    }

    @Test
    void deleteReturnsNotFoundForUnknownTask() throws Exception {
        mockMvc.perform(delete("/api/goals/{goalId}/tasks/{id}", goal.getId(), Long.MAX_VALUE)
                        .header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Task not found"));
    }

    // --- helpers ---

    private ResultActions create(Goal target, String body) throws Exception {
        return mockMvc.perform(post("/api/goals/{goalId}/tasks", target.getId())
                .header(AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions update(Task task, String body) throws Exception {
        return mockMvc.perform(put("/api/goals/{goalId}/tasks/{id}", goal.getId(), task.getId())
                .header(AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions list(Goal target) throws Exception {
        return mockMvc.perform(get("/api/goals/{goalId}/tasks", target.getId()).header(AUTHORIZATION, bearer(user)));
    }

    private ResultActions list(Goal target, String param, String value) throws Exception {
        return mockMvc.perform(get("/api/goals/{goalId}/tasks", target.getId())
                .param(param, value)
                .header(AUTHORIZATION, bearer(user)));
    }

    private void givenTasks(Goal target, int count) {
        IntStream.range(0, count).forEach(i -> fixtures.task(target, 1.0, false));
    }

    private static String taskBody(String title, Double score, Boolean completed) {
        String completedJson = completed == null ? "null" : completed.toString();
        return """
                {"title": "%s", "description": "desc", "score": %s, "completed": %s}""".formatted(title, score, completedJson);
    }

    private static String daysAgoIsoDate(int days) {
        return LocalDate.now(ZoneOffset.UTC).minusDays(days).toString();
    }
}
