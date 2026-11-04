package ru.moysklad.intern.patterns.validation;

import ru.moysklad.intern.patterns.model.OrderDraft;
import ru.moysklad.intern.patterns.model.OrderItem;

import java.math.BigDecimal;

/**
 * Правило 3: цена каждой позиции задана и неотрицательна (нулевая цена допустима, например для подарка).
 * Ошибка — {@link ValidationErrorCode#INVALID_PRICE}.
 */
public class PriceValidator extends AbstractOrderValidator {

    @Override
    public void validate(OrderDraft order) {
        for (OrderItem pos : order.items()) {
            if (pos.unitPrice() == null || pos.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new OrderValidationException(
                        ValidationErrorCode.INVALID_PRICE,
                        "Некорректная цена для товара: " + pos.productName());
            }
        }
        passToNext(order);
    }
}
