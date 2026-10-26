package ru.moysklad.intern.camunda.order.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component(RecordOrderOutcomeDelegate.BEAN_NAME)
public class RecordOrderOutcomeDelegate implements JavaDelegate {
    public static final String BEAN_NAME = "recordOrderOutcomeDelegate";

    @Override
    public void execute(DelegateExecution execution) throws Exception {

    }
}
