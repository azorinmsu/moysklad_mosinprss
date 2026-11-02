package ru.moysklad.intern.patterns.discount;

import ru.moysklad.intern.patterns.model.Money;

import java.math.BigDecimal;

/**
 * Скидка для {@link ru.moysklad.intern.patterns.model.CustomerType#WHOLESALE}:
 * 10%, если стоимость товаров не меньше {@link #THRESHOLD}; иначе 0%.
 */
public class WholesaleDiscountStrategy implements DiscountStrategy {

    public static final BigDecimal THRESHOLD = Money.of("50000");
    public static final BigDecimal RATE = new BigDecimal("0.10");

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Стоимость товаров не может быть нулевой или отрицательной");

        if (subtotal.compareTo(THRESHOLD) >= 0)
            return Money.round(subtotal.multiply(RATE));
        else
            return Money.ZERO;
    }
}
