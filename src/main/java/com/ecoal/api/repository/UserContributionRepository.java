package com.ecoal.api.repository;

import com.ecoal.api.entity.UserContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserContributionRepository extends JpaRepository<UserContribution, Long> {
    @Query("SELECT uc FROM UserContribution uc WHERE uc.user.company.id = :companyId")
    List<UserContribution> findAllByUserCompanyId(@Param("companyId") Long companyId);
}
