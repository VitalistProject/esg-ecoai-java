package com.ecoal.api.controller;

import com.ecoal.api.entity.UserAccount;
import com.ecoal.api.support.ApiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Client mistakes must be reported as 4xx; only genuine server faults may surface as 500. */
class ErrorHandlingApiTest extends ApiTest {

    private UserAccount user;

    @BeforeEach
    void createUser() {
        user = fixtures.user(fixtures.company());
    }

    @Test
    void malformedJsonBodyIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .header(AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownDimensionInBodyIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .header(AUTHORIZATION, bearer(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Goal\", \"dimension\": \"economic\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownDimensionInQueryIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/goals").param("dimension", "economic").header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidDateInQueryIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/goals").param("created_at", "yesterday").header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonNumericPathIdIsABadRequest() throws Exception {
        mockMvc.perform(get("/api/goals/abc").header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedHttpMethodIsMethodNotAllowed() throws Exception {
        mockMvc.perform(patch("/api/goals").header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unknownRouteIsNotFound() throws Exception {
        mockMvc.perform(get("/api/does-not-exist").header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unsupportedContentTypeIsUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/api/goals")
                        .header(AUTHORIZATION, bearer(user))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("title=Goal"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void errorBodiesNeverLeakInternalDetails() throws Exception {
        mockMvc.perform(get("/api/goals/abc").header(AUTHORIZATION, bearer(user)))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }
}
