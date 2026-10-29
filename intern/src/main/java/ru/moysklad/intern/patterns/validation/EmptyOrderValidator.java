package ru.moysklad.intern.patterns.validation;

import ru.moysklad.intern.patterns.model.OrderDraft;

/**
 * Правило 1: заказ содержит хотя бы одну позицию. Ошибка — {@link ValidationErrorCode#EMPTY_ORDER}.
 */
public class EmptyOrderValidator extends AbstractOrderValidator {

    @Override
    public void validate(OrderDraft order) {
        // TODO:
        // Проверить, что заказ содержит хотя бы одну позицию.
        // При нарушении — OrderValidationException с кодом EMPTY_ORDER,
        // иначе передать заказ следующему звену.
        throw new UnsupportedOperationException("TODO");
    }
}
