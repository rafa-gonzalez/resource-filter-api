package com.example.filter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

final class AndFilter implements Filter {
    private final List<Filter> children;

    AndFilter(List<Filter> children) {
        Objects.requireNonNull(children, "children can't be null");
        if (children.isEmpty()) {
            throw new IllegalArgumentException("children can't be empty");
        }
        this.children = List.copyOf(children);
    }

    @Override
    public boolean matches(Map<String, String> resource) {
        Objects.requireNonNull(resource, "resource must not be null");

        for (Filter child: children) {
            if (!child.matches(resource)) {
                return false; 
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        sb.append("(");
        for (Filter child: children) {
            if (!first) {
                sb.append(" AND ");
            }
            sb.append(child);
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }
}
