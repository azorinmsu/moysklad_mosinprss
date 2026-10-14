package ru.moysklad.intern.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessContext;
import ru.moysklad.intern.camunda.step.StockValidationStep;
import tools.jackson.databind.ObjectMapper;

@Component(CheckAvailableStockDelegate.BEAN_NAME)
public class CheckAvailableStockDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "checkAvailableStockDelegate";

    private final StockValidationStep stockValidationStep;
    private final ObjectMapper objectMapper;

    public CheckAvailableStockDelegate(StockValidationStep stockValidationStep, ObjectMapper objectMapper) {
        this.stockValidationStep = stockValidationStep;
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(DelegateExecution execution) {
        stockValidationStep.validate(new ProcessContext(execution, objectMapper));
    }
}
