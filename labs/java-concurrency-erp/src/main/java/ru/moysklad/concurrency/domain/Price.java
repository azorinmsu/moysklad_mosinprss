package ru.moysklad.concurrency.domain;

import java.math.BigDecimal;

public record Price(String productId, BigDecimal salePrice, BigDecimal discountPercent) {
}
