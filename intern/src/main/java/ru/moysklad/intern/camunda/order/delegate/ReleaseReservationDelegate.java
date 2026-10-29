package ru.moysklad.intern.camunda.order.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.order.step.ReleaseReservationStep;

@Component(ReleaseReservationDelegate.BEAN_NAME)
public class ReleaseReservationDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "releaseReservationDelegate";

    private final ReleaseReservationStep releaseReservationStep;

    public ReleaseReservationDelegate(ReleaseReservationStep releaseReservationStep) {
        this.releaseReservationStep = releaseReservationStep;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        releaseReservationStep.release(execution);
    }
}
