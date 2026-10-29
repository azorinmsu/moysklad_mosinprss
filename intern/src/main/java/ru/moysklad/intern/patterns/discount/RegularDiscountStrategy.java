package ru.moysklad.intern.patterns.discount;

import java.math.BigDecimal;

/**
 * Скидка для {@link ru.moysklad.intern.patterns.model.CustomerType#REGULAR}: 0%.
 */
public class RegularDiscountStrategy implements DiscountStrategy {

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        // TODO: Обычный покупатель не получает скидку.
        //  Проверьте входной subtotal по контракту DiscountStrategy и верните 0.00 с единым правилом округления.
        throw new UnsupportedOperationException("TODO");
    }
}
