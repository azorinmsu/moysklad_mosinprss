package ru.moysklad.intern.camunda.order.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component(CancelDemandDraftDelegate.BEAN_NAME)
public class CancelDemandDraftDelegate implements JavaDelegate {
    public static final String BEAN_NAME = "cancelDemandDraftDelegate";

    @Override
    public void execute(DelegateExecution execution) throws Exception {

    }
}
