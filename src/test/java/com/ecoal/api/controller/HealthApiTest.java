package com.ecoal.api.controller;

import com.ecoal.api.entity.UserAccount;
import com.ecoal.api.support.ApiTest;
import org.junit.jupiter.api.Test;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthApiTest extends ApiTest {

    @Test
    void healthIsPublicAndReportsUpWhenDatabaseIsReachable() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"status\":\"UP\"}", true));
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() throws Exception {
        UserAccount user = fixtures.user(fixtures.company());

        mockMvc.perform(get("/actuator/env").header(AUTHORIZATION, bearer(user)))
                .andExpect(status().isNotFound());
    }

    @Test
    void otherActuatorEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().isForbidden());
    }
}
