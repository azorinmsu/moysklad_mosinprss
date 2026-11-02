package ru.moysklad.intern.patterns.discount;

import ru.moysklad.intern.patterns.model.Money;

import java.math.BigDecimal;

/**
 * Скидка для {@link ru.moysklad.intern.patterns.model.CustomerType#VIP}: 15% независимо от суммы.
 */
public class VipDiscountStrategy implements DiscountStrategy {

    public static final BigDecimal RATE = new BigDecimal("0.15");

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Стоимость товаров не может быть нулевой или отрицательной");
        return Money.round(subtotal.multiply(RATE));
    }
}
