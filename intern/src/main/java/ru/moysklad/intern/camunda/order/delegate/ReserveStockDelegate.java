package ru.moysklad.intern.camunda.order.delegate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component(ReserveStockDelegate.BEAN_NAME)
public class ReserveStockDelegate implements JavaDelegate {
    public static final String BEAN_NAME = "reserveStockDelegate";


    @Override
    public void execute(DelegateExecution execution) throws Exception {

    }
}
