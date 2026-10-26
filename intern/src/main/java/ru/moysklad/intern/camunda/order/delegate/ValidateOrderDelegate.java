package ru.moysklad.intern.camunda.order.delegate;

import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessContext;

@Component(ValidateOrderDelegate.BEAN_NAME)
public class ValidateOrderDelegate {
    public static final String BEAN_NAME = "validateOrderDelegate";

    public void validate(ProcessContext context) {

    }
}
