package ru.moysklad.intern.patterns.pricing;

import ru.moysklad.intern.patterns.discount.DiscountStrategy;
import ru.moysklad.intern.patterns.model.Money;
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
        BigDecimal subtotal = order.subtotal();
        BigDecimal discount = discountStrategy.calculateDiscount(subtotal);
        return Money.round(subtotal.subtract(discount)); // вычитаем саму скидку
    }
}
