package com.ecoal.api.repository;

import com.ecoal.api.entity.TrainingMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainingMetricRepository extends JpaRepository<TrainingMetric, Long> {
    List<TrainingMetric> findAllByCompanyIdOrderByYearAscQuarterAsc(Long companyId);
}
