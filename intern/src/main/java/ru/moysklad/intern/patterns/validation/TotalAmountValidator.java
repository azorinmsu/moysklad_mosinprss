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
        if (order.subtotal().compareTo(MAX_TOTAL_AMOUNT) > 0) {
            throw new OrderValidationException(
                    ValidationErrorCode.TOTAL_AMOUNT_EXCEEDED,
                    "Сумма заказа превышает максимально допустимую (1 000 000 ₽)"
            );
        }
        passToNext(order);
    }
}
