package com.ecoal.api.repository;

import com.ecoal.api.entity.Company;
import com.ecoal.api.entity.Dimension;
import com.ecoal.api.entity.Goal;
import com.ecoal.api.entity.Task;
import com.ecoal.api.support.Fixtures;
import com.ecoal.api.support.OracleContainerSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(Fixtures.class)
class TaskRepositoryTest extends OracleContainerSupport {

    private static final OffsetDateTime CUTOFF = OffsetDateTime.of(2026, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private Fixtures fixtures;

    private Goal goal;

    @BeforeEach
    void createGoal() {
        Company company = fixtures.company();
        goal = fixtures.goal(company, Dimension.ENVIRONMENTAL);
    }

    @Test
    void countsOnlyTasksOfTheGivenGoal() {
        fixtures.task(goal, 1.0, false);
        fixtures.task(goal, 1.0, true);
        fixtures.task(fixtures.goal(goal.getCompany(), Dimension.SOCIAL), 1.0, false);

        assertThat(taskRepository.countByGoalId(goal.getId())).isEqualTo(2);
    }

    @Test
    void findsTaskByIdOnlyWithinItsOwnGoal() {
        Task task = fixtures.task(goal, 1.0, false);
        Goal sibling = fixtures.goal(goal.getCompany(), Dimension.SOCIAL);

        assertThat(taskRepository.findByIdAndGoalId(task.getId(), goal.getId())).isPresent();
        assertThat(taskRepository.findByIdAndGoalId(task.getId(), sibling.getId())).isEmpty();
    }

    @Test
    void filtersByCompletionStatus() {
        Task done = fixtures.task(goal, 1.0, true);
        Task pending = fixtures.task(goal, 1.0, false);
        Sort unsorted = Sort.unsorted();

        assertThat(taskRepository.findAllByGoalIdAndCompleted(goal.getId(), true, unsorted))
                .extracting(Task::getId).containsExactly(done.getId());
        assertThat(taskRepository.findAllByGoalIdAndCompleted(goal.getId(), false, unsorted))
                .extracting(Task::getId).containsExactly(pending.getId());
    }

    @Test
    void createdAfterFilterIncludesTasksCreatedExactlyAtTheCutoff() {
        Task before = fixtures.task(goal, 1.0, false);
        Task atCutoff = fixtures.task(goal, 1.0, false);
        Task after = fixtures.task(goal, 1.0, false);
        fixtures.taskCreatedAt(before, CUTOFF.minusSeconds(1));
        fixtures.taskCreatedAt(atCutoff, CUTOFF);
        fixtures.taskCreatedAt(after, CUTOFF.plusSeconds(1));

        assertThat(taskRepository.findByGoalIdAndCreatedAtAfter(goal.getId(), CUTOFF))
                .extracting(Task::getId).containsExactly(after.getId(), atCutoff.getId());
    }

    @Test
    void createdAfterFilterWithCompletionExcludesOtherStatusAndGoals() {
        Task match = fixtures.task(goal, 1.0, true);
        Task otherStatus = fixtures.task(goal, 1.0, false);
        Task otherGoal = fixtures.task(fixtures.goal(goal.getCompany(), Dimension.SOCIAL), 1.0, true);
        fixtures.taskCreatedAt(match, CUTOFF.plusDays(1));
        fixtures.taskCreatedAt(otherStatus, CUTOFF.plusDays(1));
        fixtures.taskCreatedAt(otherGoal, CUTOFF.plusDays(1));

        assertThat(taskRepository.findByGoalIdAndCompletedAndCreatedAtAfter(goal.getId(), true, CUTOFF))
                .extracting(Task::getId).containsExactly(match.getId());
    }
}
