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
        // TODO:
        //  1. Проверить subtotal по контракту DiscountStrategy.
        //  2. Если subtotal ниже THRESHOLD — скидки нет.
        //  3. Если subtotal равен THRESHOLD или выше — скидка RATE от subtotal.
        //  4. Округлить результат по единому правилу Money.
        //  Подсказка: сравнивайте BigDecimal через compareTo, а не equals.
        throw new UnsupportedOperationException("TODO");
    }
}
