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
        EmptyOrderValidator emptyOrderValidator = new EmptyOrderValidator();
        emptyOrderValidator.linkWith(new QuantityValidator())
                .linkWith(new PriceValidator())
                .linkWith(new TotalAmountValidator());
        return new OrderValidationChain(emptyOrderValidator);
    }

    public void validate(OrderDraft order) {
        Objects.requireNonNull(order, "order");
        head.validate(order);
    }
}
