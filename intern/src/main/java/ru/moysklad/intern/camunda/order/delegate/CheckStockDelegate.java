package ru.moysklad.intern.camunda.order.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.order.step.CheckStockStep;

@Component(CheckStockDelegate.BEAN_NAME)
public class CheckStockDelegate implements JavaDelegate {

    public static final String BEAN_NAME = "checkStockDelegate";

    private final CheckStockStep checkStockStep;

    public CheckStockDelegate(CheckStockStep checkStockStep) {
        this.checkStockStep = checkStockStep;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        checkStockStep.checkStock(execution);
    }
}
