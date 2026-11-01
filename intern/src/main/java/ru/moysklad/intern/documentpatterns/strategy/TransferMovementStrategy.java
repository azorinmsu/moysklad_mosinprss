package ru.moysklad.intern.documentpatterns.strategy;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.StockMovement;

import java.util.List;

/**
 * Перемещение: для каждой позиции расход со склада-источника и приход на склад-получатель.
 *
 * <pre>
 * Перемещение 5 шт. товара X с A на B  →  [A / X: -5, B / X: +5]
 * </pre>
 */
public class TransferMovementStrategy implements StockMovementStrategy {

    @Override
    public List<StockMovement> calculateMovements(ErpDocument document) {
        // TODO:
        // 1. Для каждой позиции создайте ДВА движения:
        //    a) document.getSourceWarehouse(): delta = -quantity;
        //    b) document.getTargetWarehouse(): delta = +quantity.
        // 2. Порядок: расход, затем приход — для каждой позиции по очереди.
        // 3. Атомарность обеспечит хранилище при применении всего набора; стратегия лишь считает.
        throw new UnsupportedOperationException("TODO");
    }
}
