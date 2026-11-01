package ru.moysklad.intern.documentpatterns.repository;

import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.util.Objects;
import java.util.UUID;

/**
 * Ключ остатка: товар + склад + (необязательно) партия.
 *
 * <p>Сущности {@code Product}/{@code Warehouse}/{@code Lot} не переопределяют {@code equals},
 * поэтому ключ строится по идентификаторам.
 */
public record StockKey(UUID productId, UUID warehouseId, UUID lotId) {

    public StockKey {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(warehouseId, "warehouseId");
    }

    public static StockKey of(Product product, Warehouse warehouse) {
        return of(product, warehouse, null);
    }

    public static StockKey of(Product product, Warehouse warehouse, Lot lot) {
        Objects.requireNonNull(product, "product");
        Objects.requireNonNull(warehouse, "warehouse");
        if (product.getId() == null) {
            throw new IllegalArgumentException("У товара «" + product.getName() + "» не задан id");
        }
        if (warehouse.getId() == null) {
            throw new IllegalArgumentException("У склада «" + warehouse.getName() + "» не задан id");
        }
        return new StockKey(product.getId(), warehouse.getId(), lot == null ? null : lot.getId());
    }

    public static StockKey of(StockMovement movement) {
        return of(movement.product(), movement.warehouse(), movement.lot());
    }
}
