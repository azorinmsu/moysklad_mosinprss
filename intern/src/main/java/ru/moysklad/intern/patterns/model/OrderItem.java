package ru.moysklad.intern.patterns.model;

import ru.moysklad.intern.entity.Product;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Позиция заказа покупателя: товар, цена за единицу и количество.
 * <p>
 * Конструктор намеренно не проверяет цену и количество: некорректные значения
 * должны отсекаться валидаторами заказа, а не моделью.
 *
 * @param productId   идентификатор товара ({@link Product#getId()})
 * @param productName наименование товара
 * @param unitPrice   цена за единицу, ₽
 * @param quantity    количество (как и в {@code AggregateStock}, дробное допускается)
 */
public record OrderItem(UUID productId, String productName, BigDecimal unitPrice, BigDecimal quantity) {

    /**
     * Создаёт позицию из каталожного товара МойСклад. Цена берётся из {@link Product#getPrice()}.
     */
    public static OrderItem fromProduct(Product product, BigDecimal quantity) {
        Objects.requireNonNull(product, "product");
        return new OrderItem(product.getId(), product.getName(), product.getPrice(), quantity);
    }

    /**
     * Стоимость позиции: цена × количество, округлённая по {@link Money#round(BigDecimal)}.
     * Для некорректной позиции (нет цены или количества) бросает {@link IllegalStateException}.
     */
    public BigDecimal lineTotal() {
        if (unitPrice == null || quantity == null) {
            throw new IllegalStateException("У позиции '" + productName + "' не задана цена или количество");
        }
        return Money.round(unitPrice.multiply(quantity));
    }
}
