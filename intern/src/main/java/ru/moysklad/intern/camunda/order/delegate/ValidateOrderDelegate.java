package ru.moysklad.intern.camunda.order.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.order.step.ValidateOrderStep;

@Component(ValidateOrderDelegate.BEAN_NAME)
public class ValidateOrderDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "validateOrderDelegate";

    private final ValidateOrderStep validateOrderStep;

    public ValidateOrderDelegate(ValidateOrderStep validateOrderStep) {
        this.validateOrderStep = validateOrderStep;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        validateOrderStep.validate(execution);
    }
}
