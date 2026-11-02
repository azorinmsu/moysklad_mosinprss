package ru.moysklad.intern.camunda.order.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.order.step.RecordOrderOutcomeStep;

@Component(RecordOrderOutcomeDelegate.BEAN_NAME)
public class RecordOrderOutcomeDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "recordOrderOutcomeDelegate";

    private final RecordOrderOutcomeStep recordOrderOutcomeStep;

    public RecordOrderOutcomeDelegate(RecordOrderOutcomeStep recordOrderOutcomeStep) {
        this.recordOrderOutcomeStep = recordOrderOutcomeStep;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        recordOrderOutcomeStep.recordOutcome(execution);
    }
}
