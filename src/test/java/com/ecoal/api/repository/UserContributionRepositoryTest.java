package com.ecoal.api.repository;

import com.ecoal.api.entity.Company;
import com.ecoal.api.entity.UserContribution;
import com.ecoal.api.support.Fixtures;
import com.ecoal.api.support.OracleContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(Fixtures.class)
class UserContributionRepositoryTest extends OracleContainerSupport {

    @Autowired
    private UserContributionRepository userContributionRepository;
    @Autowired
    private Fixtures fixtures;

    @Test
    void findsContributionsOfEveryUserInTheCompanyAndNoOthers() {
        Company company = fixtures.company();
        UserContribution first = fixtures.contribution(fixtures.user(company), "volunteering", 10.0);
        UserContribution second = fixtures.contribution(fixtures.user(company), "training", 20.0);
        fixtures.contribution(fixtures.user(fixtures.company()), "volunteering", 99.0);

        assertThat(userContributionRepository.findAllByUserCompanyId(company.getId()))
                .extracting(UserContribution::getId)
                .containsExactlyInAnyOrder(first.getId(), second.getId());
    }

    @Test
    void returnsNothingForCompanyWithoutContributions() {
        Company company = fixtures.company();
        fixtures.user(company);

        assertThat(userContributionRepository.findAllByUserCompanyId(company.getId())).isEmpty();
    }
}
