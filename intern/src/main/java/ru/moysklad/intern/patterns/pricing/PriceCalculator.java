package ru.moysklad.intern.patterns.pricing;

import ru.moysklad.intern.patterns.model.OrderDraft;

import java.math.BigDecimal;

/**
 * Компонент паттерна Decorator: рассчитывает стоимость заказа.
 * Результат округлён через {@link ru.moysklad.intern.patterns.model.Money#round(BigDecimal)}.
 */
public interface PriceCalculator {

    BigDecimal calculate(OrderDraft order);
}
