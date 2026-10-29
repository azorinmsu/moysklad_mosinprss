package ru.moysklad.intern.camunda.order.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.order.step.HandleCancellationStep;

@Component(HandleCancellationDelegate.BEAN_NAME)
public class HandleCancellationDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "handleCancellationDelegate";

    private final HandleCancellationStep handleCancellationStep;

    public HandleCancellationDelegate(HandleCancellationStep handleCancellationStep) {
        this.handleCancellationStep = handleCancellationStep;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        handleCancellationStep.handleCancellation(execution);
    }
}
