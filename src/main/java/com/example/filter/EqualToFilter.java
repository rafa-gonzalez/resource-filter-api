package com.example.filter;

final class EqualToFilter extends AbstractPropertyFilter {

    EqualToFilter(String property, String value) {
        super(property, value);
    }

    @Override
    protected boolean matchesComparison(int comparisonResult) {
        return comparisonResult == 0;
    }

    @Override
    protected String operator() {
        return "==";
    }
}
