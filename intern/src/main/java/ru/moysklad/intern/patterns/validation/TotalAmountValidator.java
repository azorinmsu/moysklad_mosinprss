package ru.moysklad.intern.patterns.validation;

import ru.moysklad.intern.patterns.model.Money;
import ru.moysklad.intern.patterns.model.OrderDraft;

import java.math.BigDecimal;

/**
 * Правило 4: стоимость товаров до скидки не превышает {@link #MAX_TOTAL_AMOUNT}
 * (ровно 1 000 000,00 ₽ допустимо). Ошибка — {@link ValidationErrorCode#TOTAL_AMOUNT_EXCEEDED}.
 * <p>
 * Звено рассчитывает на то, что цены и количества уже проверены предыдущими валидаторами.
 */
public class TotalAmountValidator extends AbstractOrderValidator {

    public static final BigDecimal MAX_TOTAL_AMOUNT = Money.of("1000000");

    @Override
    public void validate(OrderDraft order) {
        // TODO:
        // 1. Получить стоимость товаров заказа (OrderDraft#subtotal).
        // 2. Если она больше MAX_TOTAL_AMOUNT — OrderValidationException с кодом TOTAL_AMOUNT_EXCEEDED.
        // 3. Иначе передать заказ следующему звену.
        throw new UnsupportedOperationException("TODO");
    }
}
