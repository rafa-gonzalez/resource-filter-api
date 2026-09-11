package com.example.filter;

final class GreaterThanFilter extends AbstractPropertyFilter {

    GreaterThanFilter(String property, String value) {
        super(property, value);
    }

    @Override
    protected boolean matchesComparison(int comparisonResult) {
        return comparisonResult > 0;
    }

    @Override
    protected String operator() {
        return ">";
    }
}
