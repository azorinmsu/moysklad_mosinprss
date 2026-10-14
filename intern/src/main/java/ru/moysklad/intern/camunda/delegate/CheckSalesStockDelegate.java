package ru.moysklad.intern.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessContext;
import ru.moysklad.intern.camunda.step.SalesStockCheckStep;
import tools.jackson.databind.ObjectMapper;

@Component(CheckSalesStockDelegate.BEAN_NAME)
public class CheckSalesStockDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "checkSalesStockDelegate";

    private final SalesStockCheckStep salesStockCheckStep;
    private final ObjectMapper objectMapper;

    public CheckSalesStockDelegate(SalesStockCheckStep salesStockCheckStep, ObjectMapper objectMapper) {
        this.salesStockCheckStep = salesStockCheckStep;
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(DelegateExecution execution) {
        salesStockCheckStep.execute(new ProcessContext(execution, objectMapper));
    }
}
