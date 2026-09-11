package com.example.filter;

final class LessThanFilter extends AbstractPropertyFilter {

    LessThanFilter(String property, String value) {
        super(property, value);
    }

    @Override
    protected boolean matchesComparison(int comparisonResult) {
        return comparisonResult < 0;
    }

    @Override
    protected String operator() {
        return "<";
    }
}
