package ru.moysklad.intern.patterns.discount;

import java.math.BigDecimal;

/**
 * Скидка для {@link ru.moysklad.intern.patterns.model.CustomerType#VIP}: 15% независимо от суммы.
 */
public class VipDiscountStrategy implements DiscountStrategy {

    public static final BigDecimal RATE = new BigDecimal("0.15");

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        // TODO:
        //  1. Проверить subtotal по контракту DiscountStrategy.
        //  2. Рассчитать RATE от subtotal.
        //  3. Округлить результат по единому правилу Money.
        throw new UnsupportedOperationException("TODO");
    }
}
