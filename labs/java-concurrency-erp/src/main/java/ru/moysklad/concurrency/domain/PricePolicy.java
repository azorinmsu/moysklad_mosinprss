package ru.moysklad.concurrency.domain;

import java.math.BigDecimal;
import java.util.List;

public final class PricePolicy {

    private List<DiscountRule> rules = List.of();
    private BigDecimal margin = BigDecimal.ZERO;
    private boolean initialized;

    public List<DiscountRule> rules() {
        return rules;
    }

    public BigDecimal margin() {
        return margin;
    }

    public boolean initialized() {
        return initialized;
    }

    public void initialize(List<DiscountRule> rules, BigDecimal margin) {
        this.rules = List.copyOf(rules);
        this.margin = margin;
        this.initialized = true;
    }
}
