package com.ecoal.api.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DimensionConverterTest {

    private final DimensionConverter converter = new DimensionConverter();

    @Test
    void storesDimensionAsItsLowercaseValue() {
        assertThat(converter.convertToDatabaseColumn(Dimension.GOVERNANCE)).isEqualTo("governance");
    }

    @Test
    void readsDimensionFromStoredValue() {
        assertThat(converter.convertToEntityAttribute("social")).isEqualTo(Dimension.SOCIAL);
    }

    @Test
    void keepsNullAsNullInBothDirections() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
