package ru.moysklad.intern.documentpatterns.model;

import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.stock.Lot;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Товарная позиция документа.
 *
 * <p>Использует каталожный {@link Product} и партию {@link Lot} из существующей модели проекта.
 * Знак и величину количества запись намеренно не проверяет: это бизнес-правило звена
 * цепочки валидации, а не инвариант структуры данных.
 *
 * @param product  товар, обязателен
 * @param quantity количество, обязательно (положительность проверяет валидатор)
 * @param lot      партия, может быть {@code null} (учёт партий — задание повышенной сложности)
 */
public record DocumentPosition(Product product, BigDecimal quantity, Lot lot) {

    public DocumentPosition {
        Objects.requireNonNull(product, "product");
        Objects.requireNonNull(quantity, "quantity");
    }

    public DocumentPosition(Product product, BigDecimal quantity) {
        this(product, quantity, null);
    }
}
