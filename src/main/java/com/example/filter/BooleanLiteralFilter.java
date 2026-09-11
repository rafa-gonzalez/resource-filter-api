package com.example.filter;

import java.util.Map;

final class BooleanLiteralFilter implements Filter {
    private final boolean bool;
    
    BooleanLiteralFilter(boolean bool) {
        this.bool = bool;
    }

    @Override 
    public boolean matches(Map<String, String> resource) {
        // Return the either alwaysTrue or alwaysFalse, does not depend on resource.
        return this.bool;
    }

    @Override
    public String toString() {
        return String.valueOf(this.bool);
    }
}
