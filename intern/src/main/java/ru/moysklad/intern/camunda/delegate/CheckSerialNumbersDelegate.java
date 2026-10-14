package ru.moysklad.intern.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
/**
 * Первый шаг процесса изменения остатков.
 * Вызов существующего {@code ThingStockManager.checkThingStocks} подключается на следующем этапе.
 * Алгоритм проверки серийных номеров не меняется.
 */
@Component(CheckSerialNumbersDelegate.BEAN_NAME)
public class CheckSerialNumbersDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "checkSerialNumbersDelegate";

    @Override
    public void execute(DelegateExecution execution) {
        throw new UnsupportedOperationException("CHECK_SERIAL_NUMBERS будет подключён к checkThingStocks на следующем этапе");
    }
}
