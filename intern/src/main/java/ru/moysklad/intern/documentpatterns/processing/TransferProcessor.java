package ru.moysklad.intern.documentpatterns.processing;

import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentPosition;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.documentpatterns.repository.StockKey;
import ru.moysklad.intern.documentpatterns.strategy.StockMovementStrategy;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidationException;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Проведение перемещения.
 */
public class TransferProcessor extends AbstractDocumentProcessor {

    private final StockMovementStrategy movementStrategy;

    public TransferProcessor(ProcessingInfrastructure infrastructure, StockMovementStrategy movementStrategy) {
        super(infrastructure);
        this.movementStrategy = Objects.requireNonNull(movementStrategy, "movementStrategy");
    }

    @Override
    protected void validateSpecific(ErpDocument document) {
        Set<StockKey> seen = new HashSet<>();
        for (DocumentPosition pos : document.getPositions()) {
            StockKey k = StockKey.of(pos.product(), document.getSourceWarehouse(), pos.lot());
            if (!seen.add(k)) {
                throw new DocumentValidationException(
                        DocumentErrorCode.DUPLICATE_POSITION,
                        "Дублирующаяся позиция товара в перемещении: " + pos.product().getName());
            }
        }
    }

    @Override
    protected List<StockMovement> calculateMovements(ErpDocument document) {
        return movementStrategy.calculateMovements(document);
    }
}
