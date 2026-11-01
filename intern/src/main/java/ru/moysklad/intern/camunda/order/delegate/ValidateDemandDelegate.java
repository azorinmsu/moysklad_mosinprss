package ru.moysklad.intern.camunda.order.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.order.step.ValidateDemandStep;

@Component(ValidateDemandDelegate.BEAN_NAME)
public class ValidateDemandDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "validateDemandDelegate";

    private final ValidateDemandStep validateDemandStep;

    public ValidateDemandDelegate(ValidateDemandStep validateDemandStep) {
        this.validateDemandStep = validateDemandStep;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        validateDemandStep.validateDemand(execution);
    }
}
