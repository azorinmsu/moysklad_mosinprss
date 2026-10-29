package ru.moysklad.intern.patterns.service;

import ru.moysklad.intern.patterns.discount.DiscountStrategyFactory;
import ru.moysklad.intern.patterns.model.OrderCalculation;
import ru.moysklad.intern.patterns.model.OrderDraft;
import ru.moysklad.intern.patterns.validation.OrderValidationChain;

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
        // TODO:
        // 1. Выполнить цепочку проверок.
        // 2. Получить стратегию скидки из фабрики.
        // 3. Создать базовый калькулятор.
        // 4. Добавить выбранные декораторы.
        // 5. Рассчитать итоговую стоимость.
        // 6. Вернуть результат расчёта.
        throw new UnsupportedOperationException("TODO");
    }
}
