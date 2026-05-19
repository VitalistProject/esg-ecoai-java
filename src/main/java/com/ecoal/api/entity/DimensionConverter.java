package com.ecoal.api.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class DimensionConverter implements AttributeConverter<Dimension, String> {

    @Override
    public String convertToDatabaseColumn(Dimension attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public Dimension convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Dimension.fromValue(dbData);
    }
}
