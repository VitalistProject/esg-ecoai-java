package com.ecoal.api.repository;

import com.ecoal.api.entity.Dimension;
import com.ecoal.api.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Sort;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findAllByCompanyId(Long companyId, Sort sort);
    List<Goal> findAllByCompanyIdAndDimension(Long companyId, Dimension dimension, Sort sort);
    long countByCompanyIdAndDimension(Long companyId, Dimension dimension);
    Optional<Goal> findByIdAndCompanyId(Long id, Long companyId);

    @Query("SELECT g FROM Goal g WHERE g.company.id = :companyId AND g.createdAt >= :createdAt ORDER BY g.createdAt DESC")
    List<Goal> findByCompanyIdAndCreatedAtAfter(@Param("companyId") Long companyId,
                                                @Param("createdAt") OffsetDateTime createdAt);

    @Query("SELECT g FROM Goal g WHERE g.company.id = :companyId AND g.dimension = :dimension AND g.createdAt >= :createdAt ORDER BY g.createdAt DESC")
    List<Goal> findByCompanyIdAndDimensionAndCreatedAtAfter(@Param("companyId") Long companyId,
                                                            @Param("dimension") Dimension dimension,
                                                            @Param("createdAt") OffsetDateTime createdAt);
}
