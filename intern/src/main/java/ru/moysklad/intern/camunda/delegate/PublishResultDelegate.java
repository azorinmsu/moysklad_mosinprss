package ru.moysklad.intern.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessOutcomeRecorder;

@Component(PublishResultDelegate.BEAN_NAME)
public class PublishResultDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "publishResultDelegate";

    private final ProcessOutcomeRecorder processOutcomeRecorder;

    public PublishResultDelegate(ProcessOutcomeRecorder processOutcomeRecorder) {
        this.processOutcomeRecorder = processOutcomeRecorder;
    }

    @Override
    public void execute(DelegateExecution execution) {
        processOutcomeRecorder.recordSuccess(execution);
    }
}
