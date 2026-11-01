package ru.moysklad.intern.documentpatterns.processing;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.documentpatterns.strategy.StockMovementStrategy;

import java.util.List;
import java.util.Objects;

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
        // TODO:
        // В перемещении одна и та же пара «товар + партия» не должна встречаться дважды:
        // такие позиции нужно объединять, иначе пользователь не видит итогового количества к перемещению.
        // Сравнивайте товары и партии по id. Нарушение — DocumentValidationException с кодом DUPLICATE_POSITION.
        // (Наличие и различие складов уже проверил WarehouseValidator — не дублируйте.)
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    protected List<StockMovement> calculateMovements(ErpDocument document) {
        // TODO: делегируйте расчёт movementStrategy.
        throw new UnsupportedOperationException("TODO");
    }
}
