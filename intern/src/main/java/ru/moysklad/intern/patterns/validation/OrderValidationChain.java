package ru.moysklad.intern.patterns.validation;

import ru.moysklad.intern.patterns.model.OrderDraft;

import java.util.Objects;

/**
 * Точка входа в цепочку проверок заказа. Сервис расчёта знает только о цепочке,
 * а не о конкретных правилах и их порядке.
 */
public class OrderValidationChain {

    private final OrderValidator head;

    public OrderValidationChain(OrderValidator head) {
        this.head = Objects.requireNonNull(head, "head");
    }

    /**
     * Стандартная цепочка проверок заказа покупателя. Порядок звеньев:
     * <ol>
     *     <li>{@link EmptyOrderValidator}</li>
     *     <li>{@link QuantityValidator}</li>
     *     <li>{@link PriceValidator}</li>
     *     <li>{@link TotalAmountValidator}</li>
     * </ol>
     */
    public static OrderValidationChain createDefault() {
        // TODO:
        // 1. Создать звенья.
        // 2. Связать их в указанном порядке через AbstractOrderValidator#linkWith.
        // 3. Вернуть цепочку, начинающуюся с первого звена.
        throw new UnsupportedOperationException("TODO");
    }

    public void validate(OrderDraft order) {
        Objects.requireNonNull(order, "order");
        head.validate(order);
    }
}
