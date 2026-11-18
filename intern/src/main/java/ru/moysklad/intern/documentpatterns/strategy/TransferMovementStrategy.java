package ru.moysklad.intern.documentpatterns.strategy;

import ru.moysklad.intern.documentpatterns.model.DocumentPosition;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.StockMovement;

import java.util.ArrayList;
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
        // сразу выделим необходимое кол-во памяти под лист
        List<StockMovement> moves = new ArrayList<>(document.getPositions().size() * 2);

        for (DocumentPosition pos : document.getPositions()) {
            moves.add(new StockMovement(
                    document.getId(),
                    pos.product(),
                    document.getSourceWarehouse(),
                    pos.lot(),
                    pos.quantity().negate()
            ));

            moves.add(new StockMovement(
                    document.getId(),
                    pos.product(),
                    document.getTargetWarehouse(),
                    pos.lot(),
                    pos.quantity()
            ));
        }
        return List.copyOf(moves); // гарантируем НЕИЗМЕНЯЕМОСТЬ
    }
}
