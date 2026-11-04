package ru.moysklad.intern.patterns.validation;

import ru.moysklad.intern.patterns.model.OrderDraft;
import ru.moysklad.intern.patterns.model.OrderItem;

import java.math.BigDecimal;

/**
 * Правило 2: количество каждой позиции задано и строго больше нуля.
 * Ошибка — {@link ValidationErrorCode#INVALID_QUANTITY}.
 */
public class QuantityValidator extends AbstractOrderValidator {

    @Override
    public void validate(OrderDraft order) {
        for (OrderItem pos : order.items()) {
            if (pos.quantity() == null || pos.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new OrderValidationException(
                        ValidationErrorCode.INVALID_QUANTITY,
                        "Некорректное количество для товара: " + pos.productName()
                );
            }
        }
        passToNext(order);
    }
}
