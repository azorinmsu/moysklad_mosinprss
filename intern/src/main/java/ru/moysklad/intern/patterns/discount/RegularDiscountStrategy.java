package ru.moysklad.intern.patterns.discount;

import ru.moysklad.intern.patterns.model.Money;

import java.math.BigDecimal;

/**
 * Скидка для {@link ru.moysklad.intern.patterns.model.CustomerType#REGULAR}: 0%.
 */
public class RegularDiscountStrategy implements DiscountStrategy {

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Стоимость товаров не может быть нулевой или отрицательной");
        return Money.ZERO;
    }
}
