package com.ecoal.api.config;

import com.ecoal.api.entity.Dimension;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/** Lets query params use the same lowercase values the API serializes (e.g. {@code ?dimension=social}). */
@Component
public class StringToDimensionConverter implements Converter<String, Dimension> {

    @Override
    public Dimension convert(String source) {
        return source.isBlank() ? null : Dimension.fromValue(source);
    }
}
