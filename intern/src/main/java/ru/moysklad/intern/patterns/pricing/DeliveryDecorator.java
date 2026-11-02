package ru.moysklad.intern.patterns.pricing;

import ru.moysklad.intern.patterns.model.AdditionalService;
import ru.moysklad.intern.patterns.model.Money;
import ru.moysklad.intern.patterns.model.OrderDraft;

import java.math.BigDecimal;

/**
 * Добавляет к стоимости стоимость доставки ({@link AdditionalService#DELIVERY}, 500 ₽).
 */
public class DeliveryDecorator extends PriceCalculatorDecorator {

    public DeliveryDecorator(PriceCalculator delegate) {
        super(delegate, AdditionalService.DELIVERY);
    }

    @Override
    public BigDecimal calculate(OrderDraft order) {
        return Money.round(delegate.calculate(order).add(service.price()));
    }
}
