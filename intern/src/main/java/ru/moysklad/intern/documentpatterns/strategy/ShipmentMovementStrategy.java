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
        return document.getPositions()
                .stream()
                .map(pos -> new StockMovement(
                        document.getId(),
                        pos.product(),
                        document.getSourceWarehouse(),
                        pos.lot(),
                        pos.quantity().negate()
                )).toList();
    }
}
