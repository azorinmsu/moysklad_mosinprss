package ru.moysklad.intern.camunda.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.step.SalesRequestClaimStep;

@Component(ClaimPendingSalesDelegate.BEAN_NAME)
public class ClaimPendingSalesDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "claimPendingSalesDelegate";

    private final SalesRequestClaimStep salesRequestClaimStep;

    public ClaimPendingSalesDelegate(SalesRequestClaimStep salesRequestClaimStep) {
        this.salesRequestClaimStep = salesRequestClaimStep;
    }

    @Override
    public void execute(DelegateExecution execution) {
        salesRequestClaimStep.claimAndStart();
    }
}
