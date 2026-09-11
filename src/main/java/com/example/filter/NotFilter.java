package com.example.filter;

import java.util.Map;
import java.util.Objects;

final class NotFilter implements Filter {
    private final Filter child;
 
    NotFilter(Filter filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        this.child = filter;
    }

    @Override 
    public boolean matches(Map<String, String> resource) {
        Objects.requireNonNull(resource, "resource must not be null");
        return !child.matches(resource);
    }

    @Override
    public String toString() {
        return "NOT (" + this.child.toString() + ")";
    }
}
