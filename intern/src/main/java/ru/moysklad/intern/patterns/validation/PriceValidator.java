package ru.moysklad.intern.patterns.validation;

import ru.moysklad.intern.patterns.model.OrderDraft;

/**
 * Правило 3: цена каждой позиции задана и неотрицательна (нулевая цена допустима, например для подарка).
 * Ошибка — {@link ValidationErrorCode#INVALID_PRICE}.
 */
public class PriceValidator extends AbstractOrderValidator {

    @Override
    public void validate(OrderDraft order) {
        // TODO:
        // 1. Перебрать позиции заказа.
        // 2. Если цена не задана или отрицательная — OrderValidationException
        //    с кодом INVALID_PRICE и наименованием товара в сообщении.
        // 3. Если все цены корректны — передать заказ следующему звену.
        throw new UnsupportedOperationException("TODO");
    }
}
