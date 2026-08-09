package edu.ics240.dispatch.core;

import java.util.Objects;

/** Administrative service area. */
public record Jurisdiction(String code) {

    public Jurisdiction {
        Objects.requireNonNull(code, "jurisdiction code");
        if (code.isBlank()) {
            throw new IllegalArgumentException("jurisdiction code must not be blank");
        }
    }

    public static Jurisdiction of(String code) {
        return new Jurisdiction(code);
    }
}
