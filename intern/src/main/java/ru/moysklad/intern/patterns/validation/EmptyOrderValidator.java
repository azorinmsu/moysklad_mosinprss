package ru.moysklad.intern.patterns.validation;

import ru.moysklad.intern.patterns.model.OrderDraft;

/**
 * Правило 1: заказ содержит хотя бы одну позицию. Ошибка — {@link ValidationErrorCode#EMPTY_ORDER}.
 */
public class EmptyOrderValidator extends AbstractOrderValidator {

    @Override
    public void validate(OrderDraft order) {
        if (order.items() == null || order.items().isEmpty()) {
            throw new OrderValidationException(ValidationErrorCode.EMPTY_ORDER, "Заказ не может быть пустым");
        }
        passToNext(order);
    }
}
