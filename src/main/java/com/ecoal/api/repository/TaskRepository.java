package com.ecoal.api.repository;

import com.ecoal.api.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByGoalId(Long goalId, Sort sort);
    List<Task> findAllByGoalIdAndCompleted(Long goalId, boolean completed, Sort sort);
    long countByGoalId(Long goalId);
    Optional<Task> findByIdAndGoalId(Long id, Long goalId);

    @Query("SELECT t FROM Task t WHERE t.goal.id = :goalId AND t.createdAt >= :createdAt ORDER BY t.createdAt DESC")
    List<Task> findByGoalIdAndCreatedAtAfter(@Param("goalId") Long goalId,
                                             @Param("createdAt") OffsetDateTime createdAt);

    @Query("SELECT t FROM Task t WHERE t.goal.id = :goalId AND t.completed = :completed AND t.createdAt >= :createdAt ORDER BY t.createdAt DESC")
    List<Task> findByGoalIdAndCompletedAndCreatedAtAfter(@Param("goalId") Long goalId,
                                                         @Param("completed") boolean completed,
                                                         @Param("createdAt") OffsetDateTime createdAt);
}
