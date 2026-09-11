package com.example.filter;

import java.util.Map;
import java.util.Objects;

final class BooleanLiteralFilter implements Filter {
    private final boolean value;

    BooleanLiteralFilter(boolean value) {
        this.value = value;
    }

    @Override
    public boolean matches(Map<String, String> resource) {
        // Literal — result doesn't depend on the resource, but the non-null contract on
        // Filter.matches applies uniformly to every filter type.
        Objects.requireNonNull(resource, "resource must not be null");
        return this.value;
    }

    @Override
    public String toString() {
        return String.valueOf(this.value);
    }
}
