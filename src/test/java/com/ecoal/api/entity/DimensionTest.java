package com.ecoal.api.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DimensionTest {

    @ParameterizedTest
    @CsvSource({
            "environmental, ENVIRONMENTAL",
            "social, SOCIAL",
            "governance, GOVERNANCE",
            "ENVIRONMENTAL, ENVIRONMENTAL",
            "Social, SOCIAL"
    })
    void parsesValueIgnoringCase(String raw, Dimension expected) {
        assertThat(Dimension.fromValue(raw)).isEqualTo(expected);
    }

    @Test
    void exposesLowercaseValueForSerialization() {
        assertThat(Dimension.ENVIRONMENTAL.getValue()).isEqualTo("environmental");
        assertThat(Dimension.SOCIAL.getValue()).isEqualTo("social");
        assertThat(Dimension.GOVERNANCE.getValue()).isEqualTo("governance");
    }

    @Test
    void rejectsUnknownValue() {
        assertThatThrownBy(() -> Dimension.fromValue("economic"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown dimension: economic");
    }

    @Test
    void rejectsNullValue() {
        assertThatThrownBy(() -> Dimension.fromValue(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
