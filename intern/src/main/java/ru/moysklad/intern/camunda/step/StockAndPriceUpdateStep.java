package ru.moysklad.intern.camunda.step;

import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessContext;

/**
 * Атомарное изменение AggregateStock и Price.
 *
 * <p><b>TODO (студент):</b> реализовать {@link #execute(ProcessContext)}.
 * Одна транзакция, защита от lost update между инстансами, запрет отрицательного остатка,
 * пересчёт totalCost, откат обоих изменений при ошибке.
 */
@Component
public class StockAndPriceUpdateStep {

    public void execute(ProcessContext context) {
        throw new UnsupportedOperationException("TODO: реализовать обновление остатка и себестоимости");
    }
}
