package com.aadi.sandesh.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class AllowedOrigins {
    private final List<String> values;

    public AllowedOrigins(@Value("${sandesh.allowed-origins}") String configuredOrigins) {
        values = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        if (values.isEmpty() || values.stream().anyMatch("*"::equals)) {
            throw new IllegalArgumentException(
                    "SANDESH_ALLOWED_ORIGINS must contain one or more explicit origins; wildcards are not allowed"
            );
        }
    }

    public List<String> values() {
        return values;
    }
}
