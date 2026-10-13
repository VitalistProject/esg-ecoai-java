package com.ecoal.api.repository;

import com.ecoal.api.entity.Company;
import com.ecoal.api.entity.Dimension;
import com.ecoal.api.entity.Goal;
import com.ecoal.api.support.Fixtures;
import com.ecoal.api.support.PostgresContainerSupport;
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
class GoalRepositoryTest extends PostgresContainerSupport {

    private static final OffsetDateTime CUTOFF = OffsetDateTime.of(2026, 1, 15, 0, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private GoalRepository goalRepository;
    @Autowired
    private Fixtures fixtures;

    @Test
    void countsOnlyGoalsOfTheGivenCompanyAndDimension() {
        Company company = fixtures.company();
        fixtures.goal(company, Dimension.SOCIAL);
        fixtures.goal(company, Dimension.SOCIAL);
        fixtures.goal(company, Dimension.GOVERNANCE);
        fixtures.goal(fixtures.company(), Dimension.SOCIAL);

        assertThat(goalRepository.countByCompanyIdAndDimension(company.getId(), Dimension.SOCIAL)).isEqualTo(2);
        assertThat(goalRepository.countByCompanyIdAndDimension(company.getId(), Dimension.ENVIRONMENTAL)).isZero();
    }

    @Test
    void findsGoalByIdOnlyWithinItsOwnCompany() {
        Goal goal = fixtures.goal(fixtures.company(), Dimension.SOCIAL);
        Company other = fixtures.company();

        assertThat(goalRepository.findByIdAndCompanyId(goal.getId(), goal.getCompany().getId())).isPresent();
        assertThat(goalRepository.findByIdAndCompanyId(goal.getId(), other.getId())).isEmpty();
    }

    @Test
    void sortsByCreationDateAsRequested() {
        Company company = fixtures.company();
        Goal older = fixtures.goal(company, Dimension.SOCIAL);
        Goal newer = fixtures.goal(company, Dimension.SOCIAL);
        fixtures.goalCreatedAt(older, CUTOFF.minusDays(1));
        fixtures.goalCreatedAt(newer, CUTOFF.plusDays(1));

        assertThat(goalRepository.findAllByCompanyId(company.getId(), Sort.by(Sort.Direction.DESC, "createdAt")))
                .extracting(Goal::getId).containsExactly(newer.getId(), older.getId());
        assertThat(goalRepository.findAllByCompanyId(company.getId(), Sort.by(Sort.Direction.ASC, "createdAt")))
                .extracting(Goal::getId).containsExactly(older.getId(), newer.getId());
    }

    @Test
    void createdAfterFilterIncludesGoalsCreatedExactlyAtTheCutoff() {
        Company company = fixtures.company();
        Goal before = fixtures.goal(company, Dimension.SOCIAL);
        Goal atCutoff = fixtures.goal(company, Dimension.SOCIAL);
        Goal after = fixtures.goal(company, Dimension.SOCIAL);
        fixtures.goalCreatedAt(before, CUTOFF.minusSeconds(1));
        fixtures.goalCreatedAt(atCutoff, CUTOFF);
        fixtures.goalCreatedAt(after, CUTOFF.plusSeconds(1));

        assertThat(goalRepository.findByCompanyIdAndCreatedAtAfter(company.getId(), CUTOFF))
                .extracting(Goal::getId).containsExactly(after.getId(), atCutoff.getId());
    }

    @Test
    void createdAfterFilterWithDimensionExcludesOtherDimensionsAndCompanies() {
        Company company = fixtures.company();
        Goal match = fixtures.goal(company, Dimension.SOCIAL);
        Goal otherDimension = fixtures.goal(company, Dimension.GOVERNANCE);
        Goal otherCompany = fixtures.goal(fixtures.company(), Dimension.SOCIAL);
        fixtures.goalCreatedAt(match, CUTOFF.plusDays(1));
        fixtures.goalCreatedAt(otherDimension, CUTOFF.plusDays(1));
        fixtures.goalCreatedAt(otherCompany, CUTOFF.plusDays(1));

        assertThat(goalRepository.findByCompanyIdAndDimensionAndCreatedAtAfter(
                company.getId(), Dimension.SOCIAL, CUTOFF))
                .extracting(Goal::getId).containsExactly(match.getId());
    }
}
