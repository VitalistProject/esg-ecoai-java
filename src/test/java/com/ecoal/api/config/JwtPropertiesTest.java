package com.ecoal.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesTest {

    private static final String SECRET_32_CHARS = "0123456789abcdef0123456789abcdef";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(JwtPropertiesConfig.class);

    @Test
    void bindsSecretAndExpiration() {
        runner.withPropertyValues("jwt.secret=" + SECRET_32_CHARS, "jwt.expiration-ms=60000")
                .run(context -> {
                    JwtProperties props = context.getBean(JwtProperties.class);
                    assertThat(props.getSecret()).isEqualTo(SECRET_32_CHARS);
                    assertThat(props.getExpirationMs()).isEqualTo(60_000L);
                });
    }

    @Test
    void refusesToStartWithoutSecret() {
        runner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void refusesToStartWithBlankSecret() {
        runner.withPropertyValues("jwt.secret=").run(context -> assertThat(context).hasFailed());
    }

    @Test
    void refusesToStartWithSecretShorterThan256Bits() {
        runner.withPropertyValues("jwt.secret=" + SECRET_32_CHARS.substring(1))
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration
    @EnableConfigurationProperties(JwtProperties.class)
    static class JwtPropertiesConfig {
    }
}
