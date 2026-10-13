package com.ecoal.api.controller;

import com.ecoal.api.config.JwtProperties;
import com.ecoal.api.entity.UserAccount;
import com.ecoal.api.repository.UserAccountRepository;
import com.ecoal.api.support.ApiTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityApiTest extends ApiTest {

    private static final String PROTECTED_PATH = "/api/goals";

    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void protectedRouteAcceptsValidBearerToken() throws Exception {
        UserAccount user = fixtures.user(fixtures.company());

        mockMvc.perform(get(PROTECTED_PATH).header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk());
    }

    @Test
    void protectedRouteRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH)).andExpect(status().isForbidden());
    }

    @Test
    void protectedRouteRejectsNonBearerScheme() throws Exception {
        UserAccount user = fixtures.user(fixtures.company());
        String rawToken = bearer(user).substring("Bearer ".length());

        mockMvc.perform(get(PROTECTED_PATH).header(AUTHORIZATION, "Basic " + rawToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedRouteRejectsGarbageToken() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH).header(AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedRouteRejectsTokenSignedWithAnotherSecret() throws Exception {
        UserAccount user = fixtures.user(fixtures.company());
        String forged = tokenFor(user.getEmail(), "forged-secret-with-more-than-32-characters!", 60_000);

        mockMvc.perform(get(PROTECTED_PATH).header(AUTHORIZATION, "Bearer " + forged))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedRouteRejectsExpiredToken() throws Exception {
        UserAccount user = fixtures.user(fixtures.company());
        String expired = tokenFor(user.getEmail(), jwtProperties.getSecret(), -60_000);

        mockMvc.perform(get(PROTECTED_PATH).header(AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedRouteRejectsValidTokenOfDeletedUser() throws Exception {
        UserAccount user = fixtures.user(fixtures.company());
        String token = bearer(user);
        userAccountRepository.delete(user);

        mockMvc.perform(get(PROTECTED_PATH).header(AUTHORIZATION, token))
                .andExpect(status().isForbidden());
    }

    @Test
    void signUpAndSignInArePublicButOnlyForPost() throws Exception {
        mockMvc.perform(get("/api/auth/sign-in")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/auth/sign-up")).andExpect(status().isForbidden());
    }

    @Test
    void openApiDocumentationIsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());
    }

    @Test
    void preflightFromAnyOriginIsAllowed() throws Exception {
        mockMvc.perform(options(PROTECTED_PATH)
                        .header(ORIGIN, "https://app.example.com")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.PUT.name()))
                .andExpect(status().isOk())
                .andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "https://app.example.com"));
    }

    @Test
    void preflightForMethodOutsideTheAllowedListIsRejected() throws Exception {
        mockMvc.perform(options(PROTECTED_PATH)
                        .header(ORIGIN, "https://app.example.com")
                        .header(ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.PATCH.name()))
                .andExpect(status().isForbidden());
    }

    private static String tokenFor(String email, String secret, long lifetimeMs) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(email)
                .expiration(new Date(System.currentTimeMillis() + lifetimeMs))
                .signWith(key)
                .compact();
    }
}
