package ru.moysklad.intern.patterns.validation;

import ru.moysklad.intern.patterns.model.OrderDraft;

/**
 * Обработчик в цепочке проверок заказа (Chain of Responsibility).
 * <p>
 * Если правило нарушено — {@link OrderValidationException}, и дальше по цепочке заказ не идёт.
 */
public interface OrderValidator {

    void validate(OrderDraft order);
}
