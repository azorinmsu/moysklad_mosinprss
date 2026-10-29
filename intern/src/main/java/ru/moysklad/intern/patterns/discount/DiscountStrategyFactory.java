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
        // TODO:
        // 1. Определить тип покупателя.
        // 2. Выбрать соответствующую стратегию.
        // 3. Обработать неподдерживаемый тип.
        throw new UnsupportedOperationException("TODO");
    }
}
