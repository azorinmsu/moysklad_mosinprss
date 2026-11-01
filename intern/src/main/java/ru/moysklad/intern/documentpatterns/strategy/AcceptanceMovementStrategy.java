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
        // TODO:
        // 1. Для каждой позиции создайте StockMovement:
        //    documentId — document.getId(), склад — document.getTargetWarehouse(),
        //    товар и партия — из позиции, delta — +quantity.
        // 2. Сохраните порядок позиций.
        // 3. Верните неизменяемый список. Хранилище остатков не трогайте.
        throw new UnsupportedOperationException("TODO");
    }
}
