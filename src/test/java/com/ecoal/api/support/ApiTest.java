package com.ecoal.api.support;

import com.ecoal.api.entity.UserAccount;
import com.ecoal.api.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Full-stack base for API tests: real security filter chain, real JWTs, real services and a real Oracle database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(Fixtures.class)
public abstract class ApiTest extends OracleContainerSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected Fixtures fixtures;

    @Autowired
    private JwtService jwtService;

    protected String bearer(UserAccount user) {
        return "Bearer " + jwtService.generateToken(user);
    }
}
