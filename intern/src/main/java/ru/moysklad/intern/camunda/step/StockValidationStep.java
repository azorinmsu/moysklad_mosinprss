package ru.moysklad.intern.camunda.step;

import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessContext;
import ru.moysklad.intern.dto.kafka.StockChangePositionDto;
import ru.moysklad.intern.entity.aggregate.AggregateStock;
import ru.moysklad.intern.entity.aggregate.StockMovement;
import ru.moysklad.intern.entity.operation.DocumentsType;
import ru.moysklad.intern.repo.AggregateStockRepository;
import ru.moysklad.intern.repo.StockMovementRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Проверка количественных остатков, включая исторические.
 *
 * <p><b>TODO (студент):</b> реализовать {@link #validate(ProcessContext)}.
 * Учесть тип и дату документа, склад, партию, текущий и исторический остаток.
 * При нехватке товара завершать шаг бизнес-ошибкой через {@code context.fail}.
 * Проверка на этом шаге не заменяет повторную проверку внутри транзакции обновления.
 */
@Component
public class StockValidationStep {

    private final AggregateStockRepository aggregateStockRepository;
    private final StockMovementRepository stockMovementRepository;

    public StockValidationStep(AggregateStockRepository aggregateStockRepository, StockMovementRepository stockMovementRepository) {
        this.aggregateStockRepository = aggregateStockRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    public void validate(ProcessContext context) {
        DocumentsType docType = context.documentType();
        LocalDateTime moment = context.moment();

        if (docType == null) {
            context.fail("INVALID_DOCUMENT", "Не указан тип документа");
            return;
        }
        if (moment == null) {
            context.fail("INVALID_DOCUMENT", "Не указана дата документа");
            return;
        }

        List<StockChangePositionDto> positions = context.positions();
        if (positions == null || positions.isEmpty()) {
            context.fail("INVALID_DOCUMENT", "Позиции документа отсутствуют");
            return;
        }

        for (StockChangePositionDto pos : positions) {
            if (pos.productId() == null || pos.storeId() == null || pos.consignmentId() == null) {
                context.fail("INVALID_POSITION", "Не указан товар, склад или партия в позиции");
                return;
            }
            if (pos.quantity() == null || pos.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                context.fail("INVALID_POSITION", "Количество позиции должно быть положительным");
                return;
            }
        }

        Map<StockKey, BigDecimal> requiredDeductions = calculateRequiredDeductions(docType, positions);

        for (Map.Entry<StockKey, BigDecimal> entry : requiredDeductions.entrySet()) {
            StockKey key = entry.getKey();
            BigDecimal requiredQty = entry.getValue();

            // проверка текущего остатка в AggregateStock
            BigDecimal currentStock = aggregateStockRepository
                    .findByProduct_IdAndStore_IdAndConsignment_Id(key.productId(), key.storeId(), key.consignmentId())
                    .map(AggregateStock::getQuantity)
                    .orElse(BigDecimal.ZERO);

            if (currentStock.compareTo(requiredQty) < 0) {
                BigDecimal missing = requiredQty.subtract(currentStock);
                context.fail(
                        "INSUFFICIENT_STOCK",
                        String.format("Недостаточно текущего остатка. Требуется: %s, доступно: %s", requiredQty, currentStock),
                        missing,
                        null
                );
                return;
            }

            // проверка исторического остатка на дату проведения документа
            BigDecimal historicalStock = stockMovementRepository
                    .sumQuantityDeltaUntil(key.productId(), key.storeId(), key.consignmentId(), moment);
            if (historicalStock == null) {
                historicalStock = BigDecimal.ZERO;
            }

            if (historicalStock.compareTo(requiredQty) < 0) {
                BigDecimal missing = requiredQty.subtract(historicalStock.max(BigDecimal.ZERO));
                context.fail(
                        "INSUFFICIENT_STOCK",
                        String.format("Недостаточно остатка на дату документа %s. Требуется: %s, доступно: %s", moment, requiredQty, historicalStock),
                        missing,
                        null
                );
                return;
            }

            // проверка всех последующих точек во времени (moment, now)
            List<StockMovement> subsequentMovements = stockMovementRepository
                    .findByProduct_IdAndStore_IdAndConsignment_IdOrderByMomentAsc(key.productId(), key.storeId(), key.consignmentId());

            BigDecimal runningStock = historicalStock.subtract(requiredQty);
            BigDecimal minFutureStock = runningStock;

            for (StockMovement movement : subsequentMovements) {
                if (movement.getMoment() != null && movement.getMoment().isAfter(moment)) {
                    runningStock = runningStock.add(movement.getQuantityDelta());
                    if (runningStock.compareTo(minFutureStock) < 0) {
                        minFutureStock = runningStock;
                    }
                }
            }

            if (minFutureStock.compareTo(BigDecimal.ZERO) < 0) {
                BigDecimal missing = minFutureStock.abs();
                context.fail(
                        "INSUFFICIENT_STOCK",
                        String.format("Списание задним числом приводит к отрицательному остатку в истории (минимальный остаток: %s)", minFutureStock),
                        missing,
                        null
                );
                return;
            }
        }
    }

    private Map<StockKey, BigDecimal> calculateRequiredDeductions(DocumentsType docType, List<StockChangePositionDto> positions) {
        Map<StockKey, BigDecimal> deductions = new LinkedHashMap<>();
        for (StockChangePositionDto pos : positions) {
            BigDecimal qty = pos.quantity();
            switch (docType) {
                case SHIPMENT, SHIFT, MACHINING, MAKE -> {
                    // здесь списываем storeID со склада-источника
                    StockKey key = new StockKey(pos.productId(), pos.storeId(), pos.consignmentId());
                    deductions.merge(key, qty, BigDecimal::add);
                }
                case GOODS_ACCEPTANCE -> {} // списаний нет, только увеличение остатков
            }
        }
        return deductions;
    }

    private record StockKey(UUID productId, UUID storeId, UUID consignmentId) {
    }
}
