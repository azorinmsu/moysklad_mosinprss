package ru.moysklad.intern.patterns.service;

import ru.moysklad.intern.patterns.discount.DiscountStrategy;
import ru.moysklad.intern.patterns.discount.DiscountStrategyFactory;
import ru.moysklad.intern.patterns.model.AdditionalService;
import ru.moysklad.intern.patterns.model.Money;
import ru.moysklad.intern.patterns.model.OrderCalculation;
import ru.moysklad.intern.patterns.model.OrderDraft;
import ru.moysklad.intern.patterns.pricing.BasePriceCalculator;
import ru.moysklad.intern.patterns.pricing.DeliveryDecorator;
import ru.moysklad.intern.patterns.pricing.PackagingDecorator;
import ru.moysklad.intern.patterns.pricing.PriceCalculator;
import ru.moysklad.intern.patterns.validation.OrderValidationChain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Расчёт заказа покупателя: объединяет цепочку проверок, фабрику стратегий скидок и декораторы услуг.
 * <p>
 * Сервис не знает конкретных стратегий и правил проверки — только абстракции.
 * Класс намеренно не помечен {@code @Service}: лабораторная работает без Spring Context.
 */
public class OrderCalculationService {

    private final DiscountStrategyFactory discountStrategyFactory;
    private final OrderValidationChain validationChain;

    public OrderCalculationService(DiscountStrategyFactory discountStrategyFactory,
                                   OrderValidationChain validationChain) {
        this.discountStrategyFactory = Objects.requireNonNull(discountStrategyFactory, "discountStrategyFactory");
        this.validationChain = Objects.requireNonNull(validationChain, "validationChain");
    }

    public OrderCalculation calculate(OrderDraft order) {

        validationChain.validate(order); // проверяем заказ
        // скидка
        DiscountStrategy strategy = discountStrategyFactory.create(order.customerType());
        // калькулятор
        PriceCalculator calc = new BasePriceCalculator(strategy);

        if (order.hasService(AdditionalService.DELIVERY))
            calc = new DeliveryDecorator(calc);
        if (order.hasService(AdditionalService.GIFT_PACKAGING))
            calc = new PackagingDecorator(calc);

        BigDecimal subtotal = order.subtotal();
        BigDecimal discount = strategy.calculateDiscount(subtotal);
        BigDecimal subtotalWithDiscount =
                Money.round(subtotal.subtract(discount)); // чтобы была гарантия, что у нас точно 2 знака после запятой
        BigDecimal deliveryCost =
                order.hasService(AdditionalService.DELIVERY) ?
                        AdditionalService.DELIVERY.price()
                        : Money.ZERO;

        BigDecimal packingCost =
                order.hasService(AdditionalService.GIFT_PACKAGING) ?
                        AdditionalService.GIFT_PACKAGING.price()
                        : Money.ZERO;

        // и напоследок
        BigDecimal total = calc.calculate(order);

        return new OrderCalculation(
                subtotal,
                discount,
                subtotalWithDiscount,
                deliveryCost,
                packingCost,
                total
        );
    }
}
