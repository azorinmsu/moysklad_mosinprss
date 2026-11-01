package ru.moysklad.intern.documentpatterns.validation;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;

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
        // TODO:
        // 1. Возьмите context.plannedMovements() и оставьте только расходные (isOutgoing()).
        // 2. Сгруппируйте их по StockKey.of(movement) и просуммируйте требуемое количество:
        //    две позиции одного товара с одного склада требуют сумму, а не каждую по отдельности.
        // 3. Сравните с context.availableQuantity(key). Если не хватает —
        //    DocumentValidationException с кодом INSUFFICIENT_STOCK (в сообщении: доступно / требуется).
        // 4. Приходные движения не проверяйте. Остатки не изменяйте.
        throw new UnsupportedOperationException("TODO");
    }
}
