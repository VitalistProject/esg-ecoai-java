package com.ecoal.api.controller;

import com.ecoal.api.entity.Company;
import com.ecoal.api.entity.UserAccount;
import com.ecoal.api.repository.UserAccountRepository;
import com.ecoal.api.support.ApiTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static com.ecoal.api.support.Fixtures.PASSWORD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends ApiTest {

    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Test
    void signUpCreatesUserInTheCompanyIdentifiedByCnpj() throws Exception {
        Company company = fixtures.company();
        String email = newEmail();

        signUp(signUpBody(email, "password123", company.getCnpj()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.name").value("Carol"))
                .andExpect(jsonPath("$.user.role").value("user"))
                .andExpect(jsonPath("$.user.department").value("Finance"))
                .andExpect(jsonPath("$.user.company_id").value(company.getId().intValue()))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void signUpStoresPasswordAsBcryptHash() throws Exception {
        Company company = fixtures.company();
        String email = newEmail();

        signUp(signUpBody(email, "password123", company.getCnpj())).andExpect(status().isCreated());

        String stored = userAccountRepository.findByEmail(email).orElseThrow().getPassword();
        assertThat(stored).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", stored)).isTrue();
    }

    @Test
    void signUpDoesNotExposeThePasswordInTheResponse() throws Exception {
        Company company = fixtures.company();

        signUp(signUpBody(newEmail(), "password123", company.getCnpj()))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(content().string(not(containsString("password123"))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    void signUpReturnsTokenThatAuthenticatesFollowingRequests() throws Exception {
        Company company = fixtures.company();
        String email = newEmail();

        String body = signUp(signUpBody(email, "password123", company.getCnpj()))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");

        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void signUpAcceptsPasswordWithExactlyEightCharacters() throws Exception {
        Company company = fixtures.company();

        signUp(signUpBody(newEmail(), "12345678", company.getCnpj())).andExpect(status().isCreated());
    }

    @Test
    void signUpFailsWhenCompanyCnpjIsUnknown() throws Exception {
        signUp(signUpBody(newEmail(), "password123", "00000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Company not found"));
    }

    @Test
    void signUpFailsWhenEmailIsAlreadyRegistered() throws Exception {
        Company company = fixtures.company();
        UserAccount existing = fixtures.user(company);

        signUp(signUpBody(existing.getEmail(), "password123", company.getCnpj()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already registered"));
    }

    @Test
    void signUpRejectsPasswordShorterThanEightCharacters() throws Exception {
        Company company = fixtures.company();

        signUp(signUpBody(newEmail(), "1234567", company.getCnpj()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void signUpRejectsMalformedEmail() throws Exception {
        Company company = fixtures.company();

        signUp(signUpBody("not-an-email", "password123", company.getCnpj()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    void signUpReportsEveryMissingField() throws Exception {
        signUp("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.*", hasSize(6)))
                .andExpect(jsonPath("$.fields").value(hasKey("companyCnpj")));
    }

    @Test
    void signUpDoesNotCreateAUserWhenValidationFails() throws Exception {
        Company company = fixtures.company();
        String email = newEmail();

        signUp(signUpBody(email, "short", company.getCnpj())).andExpect(status().isBadRequest());

        assertThat(userAccountRepository.existsByEmail(email)).isFalse();
    }

    @Test
    void signInReturnsTokenAndUserForValidCredentials() throws Exception {
        UserAccount user = fixtures.user(fixtures.company(), "admin", "Operations");

        signIn(user.getEmail(), PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.id").value(user.getId().intValue()))
                .andExpect(jsonPath("$.user.role").value("admin"))
                .andExpect(jsonPath("$.user.department").value("Operations"));
    }

    @Test
    void signInTokenAuthenticatesFollowingRequests() throws Exception {
        UserAccount user = fixtures.user(fixtures.company());

        String body = signIn(user.getEmail(), PASSWORD).andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");

        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().intValue()));
    }

    @Test
    void signInRejectsWrongPassword() throws Exception {
        UserAccount user = fixtures.user(fixtures.company());

        signIn(user.getEmail(), "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void signInGivesTheSameErrorForUnknownEmailAsForWrongPassword() throws Exception {
        signIn("nobody-" + UUID.randomUUID() + "@example.com", PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }

    @Test
    void signInRejectsBlankCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/sign-in").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void meReturnsTheAuthenticatedUser() throws Exception {
        Company company = fixtures.company();
        UserAccount user = fixtures.user(company, "admin", "Legal");

        mockMvc.perform(get("/api/auth/me").header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().intValue()))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.role").value("admin"))
                .andExpect(jsonPath("$.department").value("Legal"))
                .andExpect(jsonPath("$.company_id").value(company.getId().intValue()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    private ResultActions signUp(String body) throws Exception {
        return mockMvc.perform(post("/api/auth/sign-up").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions signIn(String email, String password) throws Exception {
        String body = """
                {"email": "%s", "password": "%s"}""".formatted(email, password);
        return mockMvc.perform(post("/api/auth/sign-in").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String signUpBody(String email, String password, String companyCnpj) {
        return """
                {
                  "name": "Carol",
                  "email": "%s",
                  "password": "%s",
                  "role": "user",
                  "company_cnpj": "%s",
                  "department": "Finance"
                }""".formatted(email, password, companyCnpj);
    }

    private static String newEmail() {
        return "new-" + UUID.randomUUID() + "@example.com";
    }
}
