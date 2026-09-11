package com.example.filter;

import java.util.Map;
import java.util.Objects;

final class NotFilter implements Filter {
    private final Filter child;
 
    NotFilter(Filter child) {
        Objects.requireNonNull(child, "child must not be null");
        this.child = child;
    }

    @Override
    public boolean matches(Map<String, String> resource) {
        Objects.requireNonNull(resource, "resource must not be null");
        return !child.matches(resource);
    }

    @Override
    public String toString() {
        // Always wraps the child directly, even if already self-wrapped — accepted, see
        // DESIGN.md Decision 3.
        return "NOT (" + this.child.toString() + ")";
    }
}
