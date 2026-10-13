package com.ecoal.api.controller;

import com.ecoal.api.entity.Company;
import com.ecoal.api.entity.UserAccount;
import com.ecoal.api.repository.CompanyRepository;
import com.ecoal.api.support.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CompanyApiTest extends ApiTest {

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    void createPersistsCompanyAndReturnsIt() throws Exception {
        UserAccount caller = fixtures.user(fixtures.company());
        String cnpj = fixtures.uniqueCnpj();

        mockMvc.perform(post("/api/companies")
                        .header(AUTHORIZATION, bearer(caller))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyBody("Green Corp", cnpj, 250.0)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Green Corp"))
                .andExpect(jsonPath("$.cnpj").value(cnpj))
                .andExpect(jsonPath("$.global_score_goal").value(250.0))
                .andExpect(jsonPath("$.created_at").isNotEmpty());

        assertThat(companyRepository.findByCnpj(cnpj)).isPresent();
    }

    @Test
    void createRejectsCnpjAlreadyRegistered() throws Exception {
        Company existing = fixtures.company();
        UserAccount caller = fixtures.user(existing);

        mockMvc.perform(post("/api/companies")
                        .header(AUTHORIZATION, bearer(caller))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyBody("Clone", existing.getCnpj(), 10.0)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CNPJ already registered"));
    }

    @Test
    void createRejectsBlankNameAndCnpj() throws Exception {
        UserAccount caller = fixtures.user(fixtures.company());

        mockMvc.perform(post("/api/companies")
                        .header(AUTHORIZATION, bearer(caller))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \" \", \"cnpj\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists())
                .andExpect(jsonPath("$.fields.cnpj").exists());
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/companies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(companyBody("Anon", fixtures.uniqueCnpj(), 1.0)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getReturnsOwnCompany() throws Exception {
        Company company = fixtures.company(75.0);
        UserAccount caller = fixtures.user(company);

        mockMvc.perform(get("/api/companies/{id}", company.getId()).header(AUTHORIZATION, bearer(caller)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(company.getId().intValue()))
                .andExpect(jsonPath("$.cnpj").value(company.getCnpj()))
                .andExpect(jsonPath("$.global_score_goal").value(75.0));
    }

    @Test
    void getForbidsAnotherCompany() throws Exception {
        UserAccount caller = fixtures.user(fixtures.company());
        Company other = fixtures.company();

        mockMvc.perform(get("/api/companies/{id}", other.getId()).header(AUTHORIZATION, bearer(caller)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Access denied"));
    }

    @Test
    void getReturnsNotFoundForUnknownCompany() throws Exception {
        UserAccount caller = fixtures.user(fixtures.company());

        mockMvc.perform(get("/api/companies/{id}", Long.MAX_VALUE).header(AUTHORIZATION, bearer(caller)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Company not found"));
    }

    @Test
    void updateChangesOwnCompany() throws Exception {
        Company company = fixtures.company();
        UserAccount caller = fixtures.user(company);
        String newCnpj = fixtures.uniqueCnpj();

        update(company, caller, companyBody("Renamed", newCnpj, 500.0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.cnpj").value(newCnpj))
                .andExpect(jsonPath("$.global_score_goal").value(500.0));

        Company stored = companyRepository.findById(company.getId()).orElseThrow();
        assertThat(stored.getName()).isEqualTo("Renamed");
        assertThat(stored.getCnpj()).isEqualTo(newCnpj);
        assertThat(stored.getGlobalScoreGoal()).isEqualTo(500.0);
    }

    @Test
    void updateKeepingTheSameCnpjIsNotAConflict() throws Exception {
        Company company = fixtures.company();
        UserAccount caller = fixtures.user(company);

        update(company, caller, companyBody("Same CNPJ", company.getCnpj(), 10.0))
                .andExpect(status().isOk());
    }

    @Test
    void updateRejectsCnpjOfAnotherCompany() throws Exception {
        Company company = fixtures.company();
        UserAccount caller = fixtures.user(company);
        Company other = fixtures.company();

        update(company, caller, companyBody("Stolen", other.getCnpj(), 10.0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CNPJ already registered"));

        assertThat(companyRepository.findById(company.getId()).orElseThrow().getCnpj())
                .isEqualTo(company.getCnpj());
    }

    @Test
    void updateForbidsAnotherCompanyAndLeavesItUntouched() throws Exception {
        UserAccount caller = fixtures.user(fixtures.company());
        Company other = fixtures.company();

        update(other, caller, companyBody("Hijacked", fixtures.uniqueCnpj(), 1.0))
                .andExpect(status().isForbidden());

        assertThat(companyRepository.findById(other.getId()).orElseThrow().getName()).isEqualTo(other.getName());
    }

    @Test
    void deleteRemovesOwnCompany() throws Exception {
        Company company = fixtures.company();
        UserAccount caller = fixtures.user(company);

        mockMvc.perform(delete("/api/companies/{id}", company.getId()).header(AUTHORIZATION, bearer(caller)))
                .andExpect(status().isNoContent());

        assertThat(companyRepository.existsById(company.getId())).isFalse();
    }

    @Test
    void deleteForbidsAnotherCompanyAndKeepsIt() throws Exception {
        UserAccount caller = fixtures.user(fixtures.company());
        Company other = fixtures.company();

        mockMvc.perform(delete("/api/companies/{id}", other.getId()).header(AUTHORIZATION, bearer(caller)))
                .andExpect(status().isForbidden());

        assertThat(companyRepository.existsById(other.getId())).isTrue();
    }

    private ResultActions update(Company target, UserAccount caller, String body) throws Exception {
        return mockMvc.perform(put("/api/companies/{id}", target.getId())
                .header(AUTHORIZATION, bearer(caller))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String companyBody(String name, String cnpj, double globalScoreGoal) {
        return """
                {"name": "%s", "cnpj": "%s", "global_score_goal": %s}""".formatted(name, cnpj, globalScoreGoal);
    }
}
