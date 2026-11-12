package ru.moysklad.intern.documentpatterns.processing;

import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.documentpatterns.strategy.StockMovementStrategy;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidationException;

import java.util.List;
import java.util.Objects;

/**
 * Проведение приёмки.
 */
public class AcceptanceProcessor extends AbstractDocumentProcessor {

    private final StockMovementStrategy movementStrategy;

    public AcceptanceProcessor(ProcessingInfrastructure infrastructure, StockMovementStrategy movementStrategy) {
        super(infrastructure);
        this.movementStrategy = Objects.requireNonNull(movementStrategy, "movementStrategy");
    }

    @Override
    protected void validateSpecific(ErpDocument document) {
        if (document.getSourceWarehouse() != null)
            throw new DocumentValidationException(
                    DocumentErrorCode.UNEXPECTED_SOURCE_WAREHOUSE,
                    // я думаю понятная ошибка
                    "В документе указан склад-источник, чего не должно быть для приемки. ID: " + document.getId());
    }

    @Override
    protected List<StockMovement> calculateMovements(ErpDocument document) {
        return movementStrategy.calculateMovements(document);
    }
}
