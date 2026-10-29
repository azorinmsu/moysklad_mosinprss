package ru.moysklad.intern.patterns;

import ru.moysklad.intern.patterns.model.AdditionalService;
import ru.moysklad.intern.patterns.model.CustomerType;
import ru.moysklad.intern.patterns.model.Money;
import ru.moysklad.intern.patterns.model.OrderDraft;
import ru.moysklad.intern.patterns.model.OrderItem;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Готовые тестовые данные для лабораторной «Паттерны проектирования».
 * Здесь только данные: никакой логики скидок, проверок или расчёта.
 */
public final class OrderTestData {

    private OrderTestData() {
    }

    /** REGULAR: 2 × 1 000 + 1 × 500 = 2 500 ₽, без услуг. */
    public static OrderDraft createRegularOrder() {
        return order(CustomerType.REGULAR, Set.of(),
                item("Блокнот А5", "1000", "2"),
                item("Ручка гелевая", "500", "1"));
    }

    /** WHOLESALE: 5 × 10 000 + 2 × 2 500 = 55 000 ₽ (выше порога 50 000), с доставкой. */
    public static OrderDraft createWholesaleOrder() {
        return order(CustomerType.WHOLESALE, Set.of(AdditionalService.DELIVERY),
                item("Монитор 27\"", "10000", "5"),
                item("Кабель HDMI", "2500", "2"));
    }

    /** VIP: 2 × 10 000 (товар А) + 1 × 5 000 (товар Б) = 25 000 ₽, с доставкой и подарочной упаковкой. */
    public static OrderDraft createVipOrder() {
        return order(CustomerType.VIP, EnumSet.allOf(AdditionalService.class),
                item("Товар А", "10000", "2"),
                item("Товар Б", "5000", "1"));
    }

    /** REGULAR без позиций. */
    public static OrderDraft createEmptyOrder() {
        return order(CustomerType.REGULAR, Set.of());
    }

    /** REGULAR с одной позицией нулевого количества. */
    public static OrderDraft createOrderWithInvalidQuantity() {
        return order(CustomerType.REGULAR, Set.of(),
                item("Блокнот А5", "1000", "0"));
    }

    /** REGULAR: первая позиция корректна, вторая с отрицательным количеством. */
    public static OrderDraft createOrderWithNegativeQuantity() {
        return order(CustomerType.REGULAR, Set.of(),
                item("Блокнот А5", "1000", "1"),
                item("Ручка гелевая", "500", "-3"));
    }

    /** REGULAR с позицией отрицательной цены. */
    public static OrderDraft createOrderWithNegativePrice() {
        return order(CustomerType.REGULAR, Set.of(),
                item("Скрепки", "-10", "1"));
    }

    /** REGULAR на 1 000 000,01 ₽ — на копейку больше максимума. */
    public static OrderDraft createOrderExceedingMaxAmount() {
        return order(CustomerType.REGULAR, Set.of(),
                item("Станок ЧПУ", "1000000.01", "1"));
    }

    /** REGULAR ровно на 1 000 000,00 ₽ — граница максимума. */
    public static OrderDraft createOrderAtMaxAmount() {
        return order(CustomerType.REGULAR, Set.of(),
                item("Станок ЧПУ", "1000000", "1"));
    }

    /** Заказ с одной позицией на {@code amount} ₽ для заданного типа покупателя и услуг. */
    public static OrderDraft createOrderWithSubtotal(CustomerType customerType, String amount,
                                                     Set<AdditionalService> services) {
        return order(customerType, services, item("Товар", amount, "1"));
    }

    public static OrderDraft order(CustomerType customerType, Set<AdditionalService> services, OrderItem... items) {
        return new OrderDraft(UUID.randomUUID(), customerType, List.of(items), services);
    }

    public static OrderItem item(String name, String unitPrice, String quantity) {
        return new OrderItem(
                UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)),
                name,
                new BigDecimal(unitPrice),
                new BigDecimal(quantity));
    }

    public static BigDecimal rub(String amount) {
        return Money.of(amount);
    }
}
