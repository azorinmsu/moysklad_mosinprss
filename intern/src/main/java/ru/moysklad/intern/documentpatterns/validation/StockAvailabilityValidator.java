package ru.moysklad.intern.documentpatterns.validation;

import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.documentpatterns.repository.StockKey;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Правило: для расходных движений хватает остатков.
 *
 * <p>Звено не знает, какой документ расходный: оно смотрит на <b>плановые движения</b>
 * ({@link ValidationContext#plannedMovements()}), рассчитанные стратегией. Новый тип документа
 * проверяется без изменения этого класса.
 */
public class StockAvailabilityValidator extends AbstractDocumentValidator {

    @Override
    protected void check(ErpDocument document, ValidationContext context) {
        Map<StockKey, BigDecimal> requiredByStock = new HashMap<>();
        for (StockMovement move : context.plannedMovements()) {
            if (move.isOutgoing()) {
                StockKey k = StockKey.of(move);
                BigDecimal v = move.delta().abs();
                requiredByStock.merge(k, v, BigDecimal::add);
            }
        }
        for (Map.Entry<StockKey, BigDecimal> entry : requiredByStock.entrySet()) {
            StockKey key = entry.getKey();
            BigDecimal required = entry.getValue();
            BigDecimal available = context.availableQuantity(key);
            if (available.compareTo(required) < 0) {
                throw new DocumentValidationException(
                        DocumentErrorCode.INSUFFICIENT_STOCK,
                        String.format("Недостаточно товара на складе. Доступно: %s, требуется: %s", available, required)
                );
            }
        }
    }
}
