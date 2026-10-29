package ru.moysklad.intern.patterns.validation;

import ru.moysklad.intern.patterns.model.OrderDraft;

/**
 * Правило 2: количество каждой позиции задано и строго больше нуля.
 * Ошибка — {@link ValidationErrorCode#INVALID_QUANTITY}.
 */
public class QuantityValidator extends AbstractOrderValidator {

    @Override
    public void validate(OrderDraft order) {
        // TODO:
        // 1. Перебрать позиции заказа.
        // 2. Если количество не задано, равно нулю или отрицательное — OrderValidationException
        //    с кодом INVALID_QUANTITY и наименованием товара в сообщении.
        // 3. Если все позиции корректны — передать заказ следующему звену.
        throw new UnsupportedOperationException("TODO");
    }
}
