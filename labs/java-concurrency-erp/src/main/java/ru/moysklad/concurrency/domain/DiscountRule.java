package ru.moysklad.concurrency.domain;

import java.math.BigDecimal;

public record DiscountRule(String customerSegment, BigDecimal percent) {
}
