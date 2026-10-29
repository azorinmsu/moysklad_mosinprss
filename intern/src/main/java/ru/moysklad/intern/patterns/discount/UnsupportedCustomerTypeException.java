package ru.moysklad.intern.patterns.discount;

import ru.moysklad.intern.patterns.model.CustomerType;

/**
 * Для типа покупателя нет стратегии скидки.
 */
public class UnsupportedCustomerTypeException extends RuntimeException {

    private final CustomerType customerType;

    public UnsupportedCustomerTypeException(CustomerType customerType) {
        super("Для типа покупателя " + customerType + " не настроена стратегия скидки");
        this.customerType = customerType;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }
}
