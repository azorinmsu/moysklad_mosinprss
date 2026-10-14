package ru.moysklad.intern.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessContext;
import ru.moysklad.intern.camunda.step.SalesWriteOffStep;
import tools.jackson.databind.ObjectMapper;

@Component(WriteOffStockDelegate.BEAN_NAME)
public class WriteOffStockDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "writeOffStockDelegate";

    private final SalesWriteOffStep salesWriteOffStep;
    private final ObjectMapper objectMapper;

    public WriteOffStockDelegate(SalesWriteOffStep salesWriteOffStep, ObjectMapper objectMapper) {
        this.salesWriteOffStep = salesWriteOffStep;
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(DelegateExecution execution) {
        salesWriteOffStep.execute(new ProcessContext(execution, objectMapper));
    }
}
