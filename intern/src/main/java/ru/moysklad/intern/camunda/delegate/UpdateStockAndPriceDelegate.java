package ru.moysklad.intern.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessContext;
import ru.moysklad.intern.camunda.step.StockAndPriceUpdateStep;
import tools.jackson.databind.ObjectMapper;

@Component(UpdateStockAndPriceDelegate.BEAN_NAME)
public class UpdateStockAndPriceDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "updateStockAndPriceDelegate";

    private final StockAndPriceUpdateStep stockAndPriceUpdateStep;
    private final ObjectMapper objectMapper;

    public UpdateStockAndPriceDelegate(StockAndPriceUpdateStep stockAndPriceUpdateStep, ObjectMapper objectMapper) {
        this.stockAndPriceUpdateStep = stockAndPriceUpdateStep;
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(DelegateExecution execution) {
        stockAndPriceUpdateStep.execute(new ProcessContext(execution, objectMapper));
    }
}
