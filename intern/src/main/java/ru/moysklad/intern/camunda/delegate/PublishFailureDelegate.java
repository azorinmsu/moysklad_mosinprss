package ru.moysklad.intern.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessOutcomeRecorder;

@Component(PublishFailureDelegate.BEAN_NAME)
public class PublishFailureDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "publishFailureDelegate";

    private final ProcessOutcomeRecorder processOutcomeRecorder;

    public PublishFailureDelegate(ProcessOutcomeRecorder processOutcomeRecorder) {
        this.processOutcomeRecorder = processOutcomeRecorder;
    }

    @Override
    public void execute(DelegateExecution execution) {
        processOutcomeRecorder.recordFailure(execution);
    }
}
