package ru.moysklad.intern.patterns.model;

import java.math.BigDecimal;

/**
 * Платная дополнительная услуга к заказу. Стоимость фиксированная и не зависит от суммы заказа.
 */
public enum AdditionalService {

    DELIVERY("Доставка", Money.of("500")),
    GIFT_PACKAGING("Подарочная упаковка", Money.of("200"));

    private final String title;
    private final BigDecimal price;

    AdditionalService(String title, BigDecimal price) {
        this.title = title;
        this.price = price;
    }

    public String title() {
        return title;
    }

    public BigDecimal price() {
        return price;
    }
}
