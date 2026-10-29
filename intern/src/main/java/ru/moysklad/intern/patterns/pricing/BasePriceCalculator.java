package ru.moysklad.intern.patterns.pricing;

import ru.moysklad.intern.patterns.discount.DiscountStrategy;
import ru.moysklad.intern.patterns.model.OrderDraft;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Базовый (оборачиваемый) компонент: стоимость товаров после скидки, без дополнительных услуг.
 */
public class BasePriceCalculator implements PriceCalculator {

    private final DiscountStrategy discountStrategy;

    public BasePriceCalculator(DiscountStrategy discountStrategy) {
        this.discountStrategy = Objects.requireNonNull(discountStrategy, "discountStrategy");
    }

    @Override
    public BigDecimal calculate(OrderDraft order) {
        // TODO:
        // 1. Получить стоимость товаров заказа (OrderDraft#subtotal).
        // 2. Рассчитать скидку через discountStrategy.
        // 3. Вернуть стоимость товаров за вычетом скидки, округлённую по правилу Money.
        // Стоимость услуг здесь не учитывается — её добавляют декораторы.
        throw new UnsupportedOperationException("TODO");
    }
}
