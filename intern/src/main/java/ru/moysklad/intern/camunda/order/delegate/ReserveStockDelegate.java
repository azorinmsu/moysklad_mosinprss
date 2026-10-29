package ru.moysklad.intern.camunda.order.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.order.step.ReserveStockStep;

@Component(ReserveStockDelegate.BEAN_NAME)
public class ReserveStockDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "reserveStockDelegate";

    private final ReserveStockStep reserveStockStep;

    public ReserveStockDelegate(ReserveStockStep reserveStockStep) {
        this.reserveStockStep = reserveStockStep;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        reserveStockStep.reserve(execution);
    }
}
