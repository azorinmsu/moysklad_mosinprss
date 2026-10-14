package ru.moysklad.intern.camunda.step;

import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessContext;

/**
 * Проверка количественных остатков, включая исторические.
 *
 * <p><b>TODO (студент):</b> реализовать {@link #validate(ProcessContext)}.
 * Учесть тип и дату документа, склад, партию, текущий и исторический остаток.
 * При нехватке товара завершать шаг бизнес-ошибкой через {@code context.fail}.
 * Проверка на этом шаге не заменяет повторную проверку внутри транзакции обновления.
 */
@Component
public class StockValidationStep {

    public void validate(ProcessContext context) {
        throw new UnsupportedOperationException("TODO: реализовать проверку доступного остатка");
    }
}
