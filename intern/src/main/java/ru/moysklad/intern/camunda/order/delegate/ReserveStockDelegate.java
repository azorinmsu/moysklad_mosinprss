package ru.moysklad.intern.camunda.order.delegate;

import org.springframework.stereotype.Component;

@Component(ReserveStockDelegate.BEAN_NAME)
public class ReserveStockDelegate {
    public static final String BEAN_NAME = "reserveStockDelegate";
}
