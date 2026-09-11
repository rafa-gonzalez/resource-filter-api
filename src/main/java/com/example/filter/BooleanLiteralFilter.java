package com.example.filter;

import java.util.Map;

final class BooleanLiteralFilter implements Filter {
    private final boolean value;

    BooleanLiteralFilter(boolean value) {
        this.value = value;
    }

    @Override
    public boolean matches(Map<String, String> resource) {
        // Literal — result doesn't depend on the resource.
        return this.value;
    }

    @Override
    public String toString() {
        return String.valueOf(this.value);
    }
}
