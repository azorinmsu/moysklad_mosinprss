package ru.moysklad.intern.patterns;

import ru.moysklad.intern.patterns.model.OrderDraft;
import ru.moysklad.intern.patterns.validation.AbstractOrderValidator;

/**
 * Тестовое звено цепочки: запоминает, что до него дошёл заказ, и передаёт его дальше.
 * Позволяет проверить, что после ошибки следующие звенья не вызываются.
 */
public class RecordingOrderValidator extends AbstractOrderValidator {

    private int invocations;

    @Override
    public void validate(OrderDraft order) {
        invocations++;
        passToNext(order);
    }

    public boolean wasInvoked() {
        return invocations > 0;
    }

    public int invocations() {
        return invocations;
    }
}
