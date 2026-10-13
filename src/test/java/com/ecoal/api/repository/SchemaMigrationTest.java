package com.ecoal.api.repository;

import com.ecoal.api.entity.Company;
import com.ecoal.api.entity.Dimension;
import com.ecoal.api.entity.UserAccount;
import com.ecoal.api.support.Fixtures;
import com.ecoal.api.support.PostgresContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(Fixtures.class)
class SchemaMigrationTest extends PostgresContainerSupport {

    private static final String SEEDED_CNPJ = "12345678000100";

    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private Fixtures fixtures;

    @Test
    void seedsDevCompanyWithItsUsers() {
        Company seeded = companyRepository.findByCnpj(SEEDED_CNPJ).orElseThrow();

        assertThat(seeded.getName()).isEqualTo("EcoAl Corp");
        assertThat(userAccountRepository.existsByEmail("alice@example.com")).isTrue();
        assertThat(userAccountRepository.existsByEmail("bob@example.com")).isTrue();
    }

    @Test
    void generatesIdsBeyondTheSeededRows() {
        Company company = fixtures.company();
        UserAccount user = fixtures.user(company);

        assertThat(company.getId()).isGreaterThan(1L);
        assertThat(user.getId()).isGreaterThan(2L);
        assertThat(fixtures.goal(company, Dimension.SOCIAL).getId()).isGreaterThan(3L);
        assertThat(fixtures.task(fixtures.goal(company, Dimension.SOCIAL), 10.0, false).getId()).isGreaterThan(3L);
    }

    @Test
    void rejectsDuplicateCnpj() {
        Company existing = fixtures.company();
        Company duplicate = new Company();
        duplicate.setName("Duplicate");
        duplicate.setCnpj(existing.getCnpj());

        assertThatThrownBy(() -> companyRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateEmail() {
        UserAccount existing = fixtures.user(fixtures.company());
        UserAccount duplicate = fixtures.user(existing.getCompany());
        duplicate.setEmail(existing.getEmail());

        assertThatThrownBy(() -> userAccountRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingACompanyCascadesToItsUsers() {
        Company company = fixtures.company();
        UserAccount user = fixtures.user(company);

        companyRepository.deleteById(company.getId());
        companyRepository.flush();

        assertThat(userAccountRepository.existsByEmail(user.getEmail())).isFalse();
    }
}
