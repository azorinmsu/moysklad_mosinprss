package ru.moysklad.intern.patterns.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Черновик заказа покупателя, который нужно проверить и рассчитать.
 * <p>
 * Дополнительные услуги хранятся во множестве: одна услуга не может быть выбрана дважды.
 *
 * @param orderId            идентификатор черновика
 * @param customerType       тип покупателя; может быть {@code null}, если не указан в документе
 * @param items              позиции (неизменяемая копия; {@code null} превращается в пустой список)
 * @param additionalServices выбранные услуги (неизменяемая копия; {@code null} превращается в пустое множество)
 */
public record OrderDraft(
        UUID orderId,
        CustomerType customerType,
        List<OrderItem> items,
        Set<AdditionalService> additionalServices
) {

    public OrderDraft {
        items = items == null ? List.of() : List.copyOf(items);
        additionalServices = additionalServices == null ? Set.of() : Set.copyOf(additionalServices);
    }

    public boolean hasService(AdditionalService service) {
        return additionalServices.contains(service);
    }

    /**
     * Стоимость товаров до скидки и услуг: сумма {@link OrderItem#lineTotal()} по всем позициям.
     * Для пустого заказа — {@link Money#ZERO}.
     */
    public BigDecimal subtotal() {
        BigDecimal subtotal = Money.ZERO;
        for (OrderItem item : items) {
            subtotal = subtotal.add(item.lineTotal());
        }
        return Money.round(subtotal);
    }
}
