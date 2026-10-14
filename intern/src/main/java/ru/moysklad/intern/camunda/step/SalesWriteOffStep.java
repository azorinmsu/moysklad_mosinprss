package ru.moysklad.intern.camunda.step;

import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessContext;

/**
 * <b>TODO (студент):</b> списать товар и пересчитать себестоимость.
 * Предпочтительно общая операция с AggregateStock и Price, а не второй независимый алгоритм.
 */
@Component
public class SalesWriteOffStep {

    public void execute(ProcessContext context) {
        throw new UnsupportedOperationException("TODO: реализовать WRITE_OFF_STOCK");
    }
}
