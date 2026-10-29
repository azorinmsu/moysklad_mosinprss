package ru.moysklad.intern.camunda.order.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.order.step.CreateDemandStep;

@Component(CreateDemandDelegate.BEAN_NAME)
public class CreateDemandDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "createDemandDelegate";

    private final CreateDemandStep createDemandStep;

    public CreateDemandDelegate(CreateDemandStep createDemandStep) {
        this.createDemandStep = createDemandStep;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        createDemandStep.createDemand(execution);
    }
}
