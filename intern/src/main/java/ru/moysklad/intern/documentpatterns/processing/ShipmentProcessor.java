package ru.moysklad.intern.documentpatterns.processing;

import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.documentpatterns.strategy.StockMovementStrategy;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidationException;

import java.util.List;
import java.util.Objects;

/**
 * Проведение отгрузки.
 */
public class ShipmentProcessor extends AbstractDocumentProcessor {

    private final StockMovementStrategy movementStrategy;

    public ShipmentProcessor(ProcessingInfrastructure infrastructure, StockMovementStrategy movementStrategy) {
        super(infrastructure);
        this.movementStrategy = Objects.requireNonNull(movementStrategy, "movementStrategy");
    }

    @Override
    protected void validateSpecific(ErpDocument document) {
        // TODO:
        // У отгрузки нет склада-получателя: товар уходит покупателю.
        // Если document.getTargetWarehouse() != null — DocumentValidationException
        // с кодом UNEXPECTED_TARGET_WAREHOUSE.
        if (document.getTargetWarehouse() != null)
            throw new DocumentValidationException(
                    DocumentErrorCode.UNEXPECTED_TARGET_WAREHOUSE,
                    "В документе указан склад-получатель, чего не должно быть для отгрузки. ID: " + document.getId());
    }

    @Override
    protected List<StockMovement> calculateMovements(ErpDocument document) {
        return movementStrategy.calculateMovements(document);
    }
}
