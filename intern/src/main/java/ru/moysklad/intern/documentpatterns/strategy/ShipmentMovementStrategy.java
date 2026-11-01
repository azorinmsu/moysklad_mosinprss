package ru.moysklad.intern.documentpatterns.strategy;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.StockMovement;

import java.util.List;

/**
 * Отгрузка: расход каждой позиции со склада-источника.
 *
 * <pre>
 * Отгрузка 7 шт. товара X со склада A  →  [A / X: -7]
 * </pre>
 */
public class ShipmentMovementStrategy implements StockMovementStrategy {

    @Override
    public List<StockMovement> calculateMovements(ErpDocument document) {
        // TODO:
        // 1. Для каждой позиции создайте StockMovement на складе document.getSourceWarehouse()
        //    с delta = -quantity (BigDecimal#negate).
        // 2. Сохраните порядок позиций, верните неизменяемый список.
        // 3. Остатки здесь не проверяйте — это задача StockAvailabilityValidator и хранилища.
        throw new UnsupportedOperationException("TODO");
    }
}
