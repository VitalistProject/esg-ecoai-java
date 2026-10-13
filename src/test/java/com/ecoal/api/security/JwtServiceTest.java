package com.ecoal.api.security;

import com.ecoal.api.config.JwtProperties;
import com.ecoal.api.entity.Company;
import com.ecoal.api.entity.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-with-more-than-32-characters";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private final JwtService jwtService = serviceWith(SECRET, ONE_HOUR_MS);

    @Test
    void generatedTokenCarriesTheUserIdentity() {
        String token = jwtService.generateToken(user(7L, "alice@example.com", "admin", 3L));

        Claims claims = jwtService.extractClaims(token);

        assertThat(claims.getSubject()).isEqualTo("alice@example.com");
        assertThat(claims.get("id", Long.class)).isEqualTo(7L);
        assertThat(claims.get("role", String.class)).isEqualTo("admin");
        assertThat(claims.get("companyId", Long.class)).isEqualTo(3L);
    }

    @Test
    void extractsEmailFromSubject() {
        String token = jwtService.generateToken(user(1L, "bob@example.com", "user", 1L));

        assertThat(jwtService.extractEmail(token)).isEqualTo("bob@example.com");
    }

    @Test
    void expiresAfterConfiguredLifetime() {
        String token = jwtService.generateToken(user(1L, "bob@example.com", "user", 1L));

        Date expiration = jwtService.extractClaims(token).getExpiration();
        long lifetimeMs = expiration.getTime() - jwtService.extractClaims(token).getIssuedAt().getTime();

        assertThat(lifetimeMs).isBetween(ONE_HOUR_MS - 1_000, ONE_HOUR_MS + 1_000);
    }

    @Test
    void acceptsTokenItIssued() {
        String token = jwtService.generateToken(user(1L, "bob@example.com", "user", 1L));

        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtService other = serviceWith("another-secret-with-more-than-32-characters", ONE_HOUR_MS);
        String foreignToken = other.generateToken(user(1L, "bob@example.com", "user", 1L));

        assertThat(jwtService.isValid(foreignToken)).isFalse();
    }

    @Test
    void rejectsExpiredToken() {
        JwtService alreadyExpired = serviceWith(SECRET, -1_000L);
        String token = alreadyExpired.generateToken(user(1L, "bob@example.com", "user", 1L));

        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void rejectsTokenWhosePayloadWasTamperedWith() {
        String token = jwtService.generateToken(user(1L, "bob@example.com", "user", 1L));
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"admin@example.com\",\"role\":\"admin\"}".getBytes(StandardCharsets.UTF_8));

        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThat(jwtService.isValid(forged)).isFalse();
    }

    @Test
    void rejectsUnsignedToken() {
        String unsigned = Jwts.builder().subject("bob@example.com").compact();

        assertThat(jwtService.isValid(unsigned)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-jwt", "a.b.c", "only.two"})
    void rejectsMalformedTokens(String malformed) {
        assertThat(jwtService.isValid(malformed)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    void rejectsBlankTokens(String blank) {
        assertThat(jwtService.isValid(blank)).isFalse();
    }

    private static JwtService serviceWith(String secret, long expirationMs) {
        JwtProperties props = new JwtProperties();
        props.setSecret(secret);
        props.setExpirationMs(expirationMs);
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "props", props);
        return service;
    }

    private static UserAccount user(Long id, String email, String role, Long companyId) {
        Company company = new Company();
        company.setId(companyId);
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        user.setCompany(company);
        return user;
    }
}
