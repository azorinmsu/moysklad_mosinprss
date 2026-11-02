package ru.moysklad.intern.patterns.discount;

import ru.moysklad.intern.patterns.model.CustomerType;

/**
 * Единственное место выбора конкретной {@link DiscountStrategy} по типу покупателя.
 * <p>
 * Это учебная <b>Simple Factory</b> (параметризованная фабрика), а не GoF Factory Method:
 * здесь нет иерархии создателей, в которой подклассы переопределяют фабричный метод.
 * Различие разобрано в {@code lab/design-patterns/ARCHITECTURE.md}.
 * <p>
 * Контракт {@link #create(CustomerType)}:
 * <ul>
 *     <li>{@code null} — {@link IllegalArgumentException};</li>
 *     <li>тип без стратегии — {@link UnsupportedCustomerTypeException};</li>
 *     <li>иначе — стратегия, соответствующая типу.</li>
 * </ul>
 */
public class DiscountStrategyFactory {

    public DiscountStrategy create(CustomerType customerType) {
        if (customerType == null)
            throw new IllegalArgumentException("Тип покупателя не может быть нулевым");

        switch (customerType) {
            case REGULAR -> {
                return new RegularDiscountStrategy();
            }
            case WHOLESALE -> {
                return new WholesaleDiscountStrategy();
            }
            case VIP -> {
                return new VipDiscountStrategy();
            }
            case CORPORATE -> throw new UnsupportedCustomerTypeException(customerType); // не поддерживается
            default -> throw new UnsupportedCustomerTypeException(customerType);
        }
    }
}
