package ru.moysklad.intern.patterns.pricing;

import ru.moysklad.intern.patterns.model.AdditionalService;
import ru.moysklad.intern.patterns.model.OrderDraft;

import java.math.BigDecimal;

/**
 * Добавляет к стоимости стоимость подарочной упаковки ({@link AdditionalService#GIFT_PACKAGING}, 200 ₽).
 */
public class PackagingDecorator extends PriceCalculatorDecorator {

    public PackagingDecorator(PriceCalculator delegate) {
        super(delegate, AdditionalService.GIFT_PACKAGING);
    }

    @Override
    public BigDecimal calculate(OrderDraft order) {
        // TODO:
        // 1. Получить стоимость от delegate.
        // 2. Добавить стоимость подарочной упаковки (service.price()).
        // 3. Вернуть результат, округлённый по правилу Money.
        throw new UnsupportedOperationException("TODO");
    }
}
