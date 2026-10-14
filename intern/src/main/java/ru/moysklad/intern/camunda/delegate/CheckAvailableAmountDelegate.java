package ru.moysklad.intern.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessContext;
import ru.moysklad.intern.camunda.step.SalesAmountCheckStep;
import tools.jackson.databind.ObjectMapper;

@Component(CheckAvailableAmountDelegate.BEAN_NAME)
public class CheckAvailableAmountDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "checkAvailableAmountDelegate";

    private final SalesAmountCheckStep salesAmountCheckStep;
    private final ObjectMapper objectMapper;

    public CheckAvailableAmountDelegate(SalesAmountCheckStep salesAmountCheckStep, ObjectMapper objectMapper) {
        this.salesAmountCheckStep = salesAmountCheckStep;
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(DelegateExecution execution) {
        salesAmountCheckStep.execute(new ProcessContext(execution, objectMapper));
    }
}
