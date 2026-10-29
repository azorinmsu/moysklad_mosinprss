package ru.moysklad.intern.camunda.order.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.order.step.ConfirmDemandStep;

@Component(ConfirmDemandDelegate.BEAN_NAME)
public class ConfirmDemandDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "confirmDemandDelegate";

    private final ConfirmDemandStep confirmDemandStep;

    public ConfirmDemandDelegate(ConfirmDemandStep confirmDemandStep) {
        this.confirmDemandStep = confirmDemandStep;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        confirmDemandStep.confirmDemand(execution);
    }
}
