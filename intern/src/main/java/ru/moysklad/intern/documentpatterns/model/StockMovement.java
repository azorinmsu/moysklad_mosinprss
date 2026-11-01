package ru.moysklad.intern.documentpatterns.model;

import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Движение товара: изменение остатка {@code product} на складе {@code warehouse} на величину {@code delta}.
 *
 * <p>Положительная дельта — приход, отрицательная — расход. Движение — значение (value object):
 * оно только описывает изменение, применяет его {@code StockRepository}.
 *
 * @param documentId документ-основание
 * @param product    товар
 * @param warehouse  склад
 * @param lot        партия, может быть {@code null}
 * @param delta      изменение количества со знаком
 */
public record StockMovement(UUID documentId, Product product, Warehouse warehouse, Lot lot, BigDecimal delta) {

    public StockMovement {
        Objects.requireNonNull(product, "product");
        Objects.requireNonNull(warehouse, "warehouse");
        Objects.requireNonNull(delta, "delta");
    }

    public boolean isOutgoing() {
        return delta.signum() < 0;
    }

    public boolean isIncoming() {
        return delta.signum() > 0;
    }
}
