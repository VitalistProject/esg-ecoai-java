package com.ecoal.api.support;

import com.ecoal.api.entity.Company;
import com.ecoal.api.entity.Dimension;
import com.ecoal.api.entity.EnergyMetric;
import com.ecoal.api.entity.Goal;
import com.ecoal.api.entity.Task;
import com.ecoal.api.entity.TrainingMetric;
import com.ecoal.api.entity.UserAccount;
import com.ecoal.api.entity.UserContribution;
import com.ecoal.api.repository.CompanyRepository;
import com.ecoal.api.repository.EnergyMetricRepository;
import com.ecoal.api.repository.GoalRepository;
import com.ecoal.api.repository.TaskRepository;
import com.ecoal.api.repository.TrainingMetricRepository;
import com.ecoal.api.repository.UserAccountRepository;
import com.ecoal.api.repository.UserContributionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Persists test data through the real repositories. Every entity gets unique natural keys
 * (CNPJ, email), so tests never collide with each other or with the Flyway seed data.
 */
@Component
public class Fixtures {

    public static final String PASSWORD = "password123";

    private static final AtomicLong SEQUENCE = new AtomicLong(90_000_000_000_000L);
    private static final BCryptPasswordEncoder FAST_ENCODER = new BCryptPasswordEncoder(4);

    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private GoalRepository goalRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private EnergyMetricRepository energyMetricRepository;
    @Autowired
    private TrainingMetricRepository trainingMetricRepository;
    @Autowired
    private UserContributionRepository userContributionRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public String uniqueCnpj() {
        return String.valueOf(SEQUENCE.incrementAndGet());
    }

    public Company company() {
        return company(100.0);
    }

    public Company company(Double globalScoreGoal) {
        Company company = new Company();
        company.setName("Company " + SEQUENCE.incrementAndGet());
        company.setCnpj(uniqueCnpj());
        company.setGlobalScoreGoal(globalScoreGoal);
        return companyRepository.save(company);
    }

    public UserAccount user(Company company) {
        return user(company, "user", "Engineering");
    }

    public UserAccount user(Company company, String role, String department) {
        long n = SEQUENCE.incrementAndGet();
        UserAccount user = new UserAccount();
        user.setName("User " + n);
        user.setEmail("user-" + n + "@example.com");
        user.setPassword(FAST_ENCODER.encode(PASSWORD));
        user.setRole(role);
        user.setCompany(company);
        user.setDepartment(department);
        return userAccountRepository.save(user);
    }

    public Goal goal(Company company, Dimension dimension) {
        Goal goal = new Goal();
        goal.setTitle("Goal " + SEQUENCE.incrementAndGet());
        goal.setDimension(dimension);
        goal.setCompany(company);
        return goalRepository.save(goal);
    }

    public Task task(Goal goal, Double score, boolean completed) {
        Task task = new Task();
        task.setTitle("Task " + SEQUENCE.incrementAndGet());
        task.setScore(score);
        task.setCompleted(completed);
        task.setCompletedAt(completed ? OffsetDateTime.now(ZoneOffset.UTC) : null);
        task.setGoal(goal);
        return taskRepository.save(task);
    }

    public EnergyMetric energy(Company company, String month, int year, double realized, double goal) {
        EnergyMetric metric = new EnergyMetric();
        metric.setCompany(company);
        metric.setMonth(month);
        metric.setYear(year);
        metric.setRealizedValue(realized);
        metric.setGoalValue(goal);
        return energyMetricRepository.save(metric);
    }

    public TrainingMetric training(Company company, String quarter, int year, double totalHours) {
        TrainingMetric metric = new TrainingMetric();
        metric.setCompany(company);
        metric.setQuarter(quarter);
        metric.setYear(year);
        metric.setTotalHours(totalHours);
        return trainingMetricRepository.save(metric);
    }

    public UserContribution contribution(UserAccount user, String category, double points) {
        UserContribution contribution = new UserContribution();
        contribution.setUser(user);
        contribution.setCategory(category);
        contribution.setPoints(points);
        return userContributionRepository.save(contribution);
    }

    /** created_at is set by Hibernate on insert, so tests that need old rows rewrite it in the database. */
    public void goalCreatedAt(Goal goal, OffsetDateTime createdAt) {
        jdbcTemplate.update("UPDATE goals SET created_at = ? WHERE id = ?", createdAt, goal.getId());
    }

    public void taskCreatedAt(Task task, OffsetDateTime createdAt) {
        jdbcTemplate.update("UPDATE tasks SET created_at = ? WHERE id = ?", createdAt, task.getId());
    }

    public void backdateGoal(Goal goal, int daysAgo) {
        goalCreatedAt(goal, OffsetDateTime.now(ZoneOffset.UTC).minusDays(daysAgo));
    }

    public void backdateTask(Task task, int daysAgo) {
        taskCreatedAt(task, OffsetDateTime.now(ZoneOffset.UTC).minusDays(daysAgo));
    }
}
