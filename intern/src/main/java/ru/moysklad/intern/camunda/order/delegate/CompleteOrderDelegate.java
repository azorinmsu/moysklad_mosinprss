package ru.moysklad.intern.camunda.order.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.order.step.CompleteOrderStep;

@Component(CompleteOrderDelegate.BEAN_NAME)
public class CompleteOrderDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "completeOrderDelegate";

    private final CompleteOrderStep completeOrderStep;

    public CompleteOrderDelegate(CompleteOrderStep completeOrderStep) {
        this.completeOrderStep = completeOrderStep;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        completeOrderStep.completeOrder(execution);
    }
}
