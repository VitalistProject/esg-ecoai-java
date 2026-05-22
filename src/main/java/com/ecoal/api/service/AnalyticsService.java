package com.ecoal.api.service;

import com.ecoal.api.dto.analytics.DashboardResponse;
import com.ecoal.api.dto.analytics.DimensionProgressResponse;
import com.ecoal.api.dto.analytics.ScoreResponse;
import com.ecoal.api.entity.Company;
import com.ecoal.api.entity.Dimension;
import com.ecoal.api.entity.EnergyMetric;
import com.ecoal.api.entity.Goal;
import com.ecoal.api.entity.Task;
import com.ecoal.api.entity.TrainingMetric;
import com.ecoal.api.entity.UserContribution;
import com.ecoal.api.exception.NotFoundException;
import com.ecoal.api.repository.CompanyRepository;
import com.ecoal.api.repository.EnergyMetricRepository;
import com.ecoal.api.repository.GoalRepository;
import com.ecoal.api.repository.TaskRepository;
import com.ecoal.api.repository.TrainingMetricRepository;
import com.ecoal.api.repository.UserAccountRepository;
import com.ecoal.api.repository.UserContributionRepository;
import com.ecoal.api.security.CurrentUserProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;

import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final GoalRepository goalRepository;
    private final TaskRepository taskRepository;
    private final CompanyRepository companyRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserContributionRepository userContributionRepository;
    private final EnergyMetricRepository energyMetricRepository;
    private final TrainingMetricRepository trainingMetricRepository;

    public AnalyticsService(
            GoalRepository goalRepository,
            TaskRepository taskRepository,
            CompanyRepository companyRepository,
            UserAccountRepository userAccountRepository,
            UserContributionRepository userContributionRepository,
            EnergyMetricRepository energyMetricRepository,
            TrainingMetricRepository trainingMetricRepository) {
        this.goalRepository = goalRepository;
        this.taskRepository = taskRepository;
        this.companyRepository = companyRepository;
        this.userAccountRepository = userAccountRepository;
        this.userContributionRepository = userContributionRepository;
        this.energyMetricRepository = energyMetricRepository;
        this.trainingMetricRepository = trainingMetricRepository;
    }

    public DimensionProgressResponse dimensions(String period) {
        Long companyId = CurrentUserProvider.get().getCompanyId();

        LocalDate fromDate = switch (period) {
            case "annual" -> LocalDate.now().minusYears(1);
            case "quarterly" -> LocalDate.now().minusMonths(3);
            default -> LocalDate.now().minusMonths(1);
        };
        OffsetDateTime from = fromDate.atStartOfDay().atOffset(ZoneOffset.UTC);

        List<DimensionProgressResponse.DimensionEntry> entries = new ArrayList<>();

        for (Dimension dim : Dimension.values()) {
            List<Goal> goals = goalRepository.findByCompanyIdAndDimensionAndCreatedAtAfter(companyId, dim, from);

            int total = 0;
            int completed = 0;

            for (Goal goal : goals) {
                List<Task> tasks = taskRepository.findAllByGoalId(goal.getId(), Sort.unsorted());
                total += tasks.size();
                completed += (int) tasks.stream().filter(t -> Boolean.TRUE.equals(t.getCompleted())).count();
            }

            double pct = total > 0 ? (double) completed / total * 100 : 0.0;
            entries.add(new DimensionProgressResponse.DimensionEntry(dim.getValue(), completed, total, pct));
        }

        return new DimensionProgressResponse(entries);
    }

    public ScoreResponse score() {
        Long companyId = CurrentUserProvider.get().getCompanyId();

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        List<Goal> allGoals = goalRepository.findAllByCompanyId(companyId, Sort.unsorted());

        double totalScore = 0.0;
        for (Goal goal : allGoals) {
            List<Task> tasks = taskRepository.findAllByGoalId(goal.getId(), Sort.unsorted());
            for (Task task : tasks) {
                if (Boolean.TRUE.equals(task.getCompleted()) && task.getScore() != null) {
                    totalScore += task.getScore();
                }
            }
        }

        double globalGoal = company.getGlobalScoreGoal() != null ? company.getGlobalScoreGoal() : 0.0;
        double percentage = globalGoal > 0 ? totalScore / globalGoal * 100 : 0.0;

        return new ScoreResponse(totalScore, globalGoal, percentage);
    }

    public DashboardResponse dashboard() {
        Long companyId = CurrentUserProvider.get().getCompanyId();

        // Departamentos: agrupa todos os usuários da empresa por departamento e conta
        Map<String, Long> departmentCounts = userAccountRepository.findAll()
                .stream()
                .filter(u -> u.getCompany() != null && companyId.equals(u.getCompany().getId()))
                .collect(Collectors.groupingBy(
                        u -> u.getDepartment() != null ? u.getDepartment() : "",
                        Collectors.counting()
                ));

        List<DashboardResponse.DepartmentEntry> departments = departmentCounts.entrySet()
                .stream()
                .map(e -> new DashboardResponse.DepartmentEntry(e.getKey(), e.getValue()))
                .toList();

        // Contribuições: agrupa por categoria e soma os pontos
        List<UserContribution> contributions = userContributionRepository.findAllByUserCompanyId(companyId);

        Map<String, Double> contributionByCategory = contributions.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getCategory() != null ? c.getCategory() : "",
                        Collectors.summingDouble(c -> c.getPoints() != null ? c.getPoints() : 0.0)
                ));

        List<DashboardResponse.ContributionEntry> contributionEntries = contributionByCategory.entrySet()
                .stream()
                .map(e -> new DashboardResponse.ContributionEntry(e.getKey(), e.getValue()))
                .toList();

        // Métricas de energia
        List<EnergyMetric> energyMetrics =
                energyMetricRepository.findAllByCompanyIdOrderByYearAscMonthAsc(companyId);

        List<DashboardResponse.EnergyEntry> energyEntries = energyMetrics.stream()
                .map(em -> new DashboardResponse.EnergyEntry(
                        em.getMonth(),
                        em.getYear() != null ? em.getYear() : 0,
                        em.getRealizedValue() != null ? em.getRealizedValue() : 0.0,
                        em.getGoalValue() != null ? em.getGoalValue() : 0.0
                ))
                .toList();

        // Métricas de treinamento
        List<TrainingMetric> trainingMetrics =
                trainingMetricRepository.findAllByCompanyIdOrderByYearAscQuarterAsc(companyId);

        List<DashboardResponse.TrainingEntry> trainingEntries = trainingMetrics.stream()
                .map(tm -> new DashboardResponse.TrainingEntry(
                        tm.getQuarter(),
                        tm.getYear() != null ? tm.getYear() : 0,
                        tm.getTotalHours() != null ? tm.getTotalHours() : 0.0
                ))
                .toList();

        return new DashboardResponse(departments, contributionEntries, energyEntries, trainingEntries);
    }
}
