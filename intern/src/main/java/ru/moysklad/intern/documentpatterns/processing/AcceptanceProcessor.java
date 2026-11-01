package ru.moysklad.intern.documentpatterns.processing;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.documentpatterns.strategy.StockMovementStrategy;

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
        // TODO:
        // У приёмки нет склада-источника: товар приходит от поставщика.
        // Если document.getSourceWarehouse() != null — DocumentValidationException
        // с кодом UNEXPECTED_SOURCE_WAREHOUSE.
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    protected List<StockMovement> calculateMovements(ErpDocument document) {
        // TODO: делегируйте расчёт movementStrategy. Не дублируйте логику стратегии здесь.
        throw new UnsupportedOperationException("TODO");
    }
}
