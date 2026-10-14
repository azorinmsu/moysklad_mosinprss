package ru.moysklad.intern.camunda.step;

import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessContext;

/**
 * <b>TODO (студент):</b> сравнить доступную сумму с quantity × salePrice.
 * При нехватке: errorCode {@code INSUFFICIENT_FUNDS}, missingAmount = required - available.
 */
@Component
public class SalesAmountCheckStep {

    public void execute(ProcessContext context) {
        throw new UnsupportedOperationException("TODO: реализовать CHECK_AVAILABLE_AMOUNT");
    }
}
