package com.ecoal.api.service;

import com.ecoal.api.dto.task.TaskRequest;
import com.ecoal.api.dto.task.TaskResponse;
import com.ecoal.api.entity.Goal;
import com.ecoal.api.entity.Task;
import com.ecoal.api.exception.BusinessRuleException;
import com.ecoal.api.exception.NotFoundException;
import com.ecoal.api.repository.GoalRepository;
import com.ecoal.api.repository.TaskRepository;
import com.ecoal.api.security.CurrentUserProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.data.domain.Sort;

import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final GoalRepository goalRepository;

    public TaskService(TaskRepository taskRepository, GoalRepository goalRepository) {
        this.taskRepository = taskRepository;
        this.goalRepository = goalRepository;
    }

    public List<TaskResponse> list(Long goalId, Boolean completed, LocalDate createdAt) {
        resolveGoal(goalId);
        OffsetDateTime from = createdAt != null ? createdAt.atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        List<Task> tasks;
        if (completed == null && from == null) {
            tasks = taskRepository.findAllByGoalId(goalId, sort);
        } else if (completed != null && from == null) {
            tasks = taskRepository.findAllByGoalIdAndCompleted(goalId, completed, sort);
        } else if (completed == null) {
            tasks = taskRepository.findByGoalIdAndCreatedAtAfter(goalId, from);
        } else {
            tasks = taskRepository.findByGoalIdAndCompletedAndCreatedAtAfter(goalId, completed, from);
        }
        return tasks.stream().map(this::toResponse).toList();
    }

    public TaskResponse create(Long goalId, TaskRequest request) {
        Goal goal = resolveGoal(goalId);

        long count = taskRepository.countByGoalId(goalId);
        if (count >= 10) {
            throw new BusinessRuleException("Maximum 10 tasks per goal");
        }

        Task task = new Task();
        task.setGoal(goal);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setScore(request.getScore());
        task.setCompleted(false);

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    public TaskResponse get(Long goalId, Long id) {
        resolveGoal(goalId);
        Task task = taskRepository.findByIdAndGoalId(id, goalId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        return toResponse(task);
    }

    public TaskResponse update(Long goalId, Long id, TaskRequest request) {
        resolveGoal(goalId);
        Task task = taskRepository.findByIdAndGoalId(id, goalId)
                .orElseThrow(() -> new NotFoundException("Task not found"));

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setScore(request.getScore());

        if (request.getCompleted() != null) {
            if (request.getCompleted() && !Boolean.TRUE.equals(task.getCompleted())) {
                task.setCompleted(true);
                task.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
            } else if (!request.getCompleted() && Boolean.TRUE.equals(task.getCompleted())) {
                task.setCompleted(false);
                task.setCompletedAt(null);
            }
        }

        Task saved = taskRepository.save(task);
        return toResponse(saved);
    }

    public void delete(Long goalId, Long id) {
        resolveGoal(goalId);
        Task task = taskRepository.findByIdAndGoalId(id, goalId)
                .orElseThrow(() -> new NotFoundException("Task not found"));
        taskRepository.delete(task);
    }

    private Goal resolveGoal(Long goalId) {
        Long companyId = CurrentUserProvider.get().getCompanyId();
        return goalRepository.findByIdAndCompanyId(goalId, companyId)
                .orElseThrow(() -> new NotFoundException("Goal not found"));
    }

    private TaskResponse toResponse(Task task) {
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
