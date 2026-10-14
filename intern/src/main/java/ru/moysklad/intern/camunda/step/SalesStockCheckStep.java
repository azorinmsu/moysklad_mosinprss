package ru.moysklad.intern.camunda.step;

import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessContext;

/**
 * <b>TODO (студент):</b> проверить доступное количество для заявки на реализацию.
 * При нехватке: errorCode {@code INSUFFICIENT_STOCK}, missingQuantity = requested - available.
 */
@Component
public class SalesStockCheckStep {

    public void execute(ProcessContext context) {
        throw new UnsupportedOperationException("TODO: реализовать CHECK_SALES_STOCK");
    }
}
