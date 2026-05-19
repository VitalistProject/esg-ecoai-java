package com.ecoal.api.repository;

import com.ecoal.api.entity.EnergyMetric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnergyMetricRepository extends JpaRepository<EnergyMetric, Long> {
    List<EnergyMetric> findAllByCompanyIdOrderByYearAscMonthAsc(Long companyId);
}
