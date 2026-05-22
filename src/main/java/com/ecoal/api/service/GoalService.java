package com.ecoal.api.service;

import com.ecoal.api.dto.goal.GoalRequest;
import com.ecoal.api.dto.goal.GoalResponse;
import com.ecoal.api.dto.task.TaskResponse;
import com.ecoal.api.entity.Company;
import com.ecoal.api.entity.Dimension;
import com.ecoal.api.entity.Goal;
import com.ecoal.api.entity.Task;
import com.ecoal.api.exception.BusinessRuleException;
import com.ecoal.api.exception.NotFoundException;
import com.ecoal.api.repository.CompanyRepository;
import com.ecoal.api.repository.GoalRepository;
import com.ecoal.api.security.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class GoalService {

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private CompanyRepository companyRepository;

    public List<GoalResponse> list(Dimension dimension, LocalDate createdAt) {
        Long companyId = CurrentUserProvider.get().getCompanyId();
        OffsetDateTime from = createdAt != null ? createdAt.atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        List<Goal> goals;
        if (dimension == null && from == null) {
            goals = goalRepository.findAllByCompanyId(companyId, sort);
        } else if (dimension != null && from == null) {
            goals = goalRepository.findAllByCompanyIdAndDimension(companyId, dimension, sort);
        } else if (dimension == null) {
            goals = goalRepository.findByCompanyIdAndCreatedAtAfter(companyId, from);
        } else {
            goals = goalRepository.findByCompanyIdAndDimensionAndCreatedAtAfter(companyId, dimension, from);
        }
        return goals.stream().map(g -> toResponse(g, false)).toList();
    }

    public GoalResponse create(GoalRequest request) {
        Long companyId = CurrentUserProvider.get().getCompanyId();

        long count = goalRepository.countByCompanyIdAndDimension(companyId, request.getDimension());
        if (count >= 10) {
            throw new BusinessRuleException("Maximum 10 goals per dimension");
        }

        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new NotFoundException("Company not found"));

        Goal goal = new Goal();
        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
        goal.setDimension(request.getDimension());
        goal.setCompany(company);

        Goal saved = goalRepository.save(goal);
        return toResponse(saved, false);
    }

    public GoalResponse getWithTasks(Long id) {
        Long companyId = CurrentUserProvider.get().getCompanyId();
        Goal goal = goalRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Goal not found"));
        return toResponse(goal, true);
    }

    public GoalResponse update(Long id, GoalRequest request) {
        Long companyId = CurrentUserProvider.get().getCompanyId();
        Goal goal = goalRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Goal not found"));

        if (!goal.getDimension().equals(request.getDimension())) {
            long countInNewDimension = goalRepository.countByCompanyIdAndDimension(companyId, request.getDimension());
            if (countInNewDimension >= 10) {
                throw new BusinessRuleException("Maximum 10 goals per dimension");
            }
        }

        goal.setTitle(request.getTitle());
        goal.setDescription(request.getDescription());
        goal.setDimension(request.getDimension());

        Goal saved = goalRepository.save(goal);
        return toResponse(saved, false);
    }

    public void delete(Long id) {
        Long companyId = CurrentUserProvider.get().getCompanyId();
        Goal goal = goalRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new NotFoundException("Goal not found"));
        goalRepository.delete(goal);
    }

    private GoalResponse toResponse(Goal goal, boolean includeTasks) {
        GoalResponse response = new GoalResponse();
        response.setId(goal.getId());
        response.setTitle(goal.getTitle());
        response.setDescription(goal.getDescription());
        response.setDimension(goal.getDimension());
        response.setCompanyId(goal.getCompany().getId());
        response.setCreatedAt(goal.getCreatedAt());
        response.setUpdatedAt(goal.getUpdatedAt());

        if (includeTasks) {
            List<TaskResponse> taskResponses = goal.getTasks()
                    .stream()
                    .map(this::toTaskResponse)
                    .toList();
            response.setTasks(taskResponses);
        } else {
            response.setTasks(null);
        }

        return response;
    }

    private TaskResponse toTaskResponse(Task task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setScore(task.getScore());
        response.setCompleted(task.getCompleted());
        response.setCompletedAt(task.getCompletedAt());
        response.setGoalId(task.getGoal().getId());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());
        return response;
    }
}
