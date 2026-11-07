package ru.moysklad.intern.documentpatterns.strategy;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.StockMovement;

import java.util.List;

/**
 * Приёмка: приход каждой позиции на склад-получатель.
 *
 * <pre>
 * Приёмка 10 шт. товара X на склад A  →  [A / X: +10]
 * </pre>
 */
public class AcceptanceMovementStrategy implements StockMovementStrategy {

    @Override
    public List<StockMovement> calculateMovements(ErpDocument document) {
        return document.getPositions()
                .stream()
                .map(pos -> new StockMovement(
                        document.getId(),
                        pos.product(),
                        document.getTargetWarehouse(),
                        pos.lot(),
                        pos.quantity()
                )).toList();
    }
}
