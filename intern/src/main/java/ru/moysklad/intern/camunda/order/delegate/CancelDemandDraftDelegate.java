package ru.moysklad.intern.camunda.order.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.order.step.CancelDemandDraftStep;

@Component(CancelDemandDraftDelegate.BEAN_NAME)
public class CancelDemandDraftDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "cancelDemandDraftDelegate";

    private final CancelDemandDraftStep cancelDemandDraftStep;

    public CancelDemandDraftDelegate(CancelDemandDraftStep cancelDemandDraftStep) {
        this.cancelDemandDraftStep = cancelDemandDraftStep;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        cancelDemandDraftStep.cancelDemand(execution);
    }
}
