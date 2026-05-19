package com.ecoal.api.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Dimension {
    ENVIRONMENTAL("environmental"),
    SOCIAL("social"),
    GOVERNANCE("governance");

    private final String value;

    Dimension(String value) { this.value = value; }

    @JsonValue
    public String getValue() { return value; }

    @JsonCreator
    public static Dimension fromValue(String value) {
        for (Dimension d : values()) {
            if (d.value.equalsIgnoreCase(value)) return d;
        }
        throw new IllegalArgumentException("Unknown dimension: " + value);
    }
}
