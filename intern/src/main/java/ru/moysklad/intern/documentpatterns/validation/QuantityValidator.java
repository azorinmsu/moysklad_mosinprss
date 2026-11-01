package ru.moysklad.intern.documentpatterns.validation;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;

/**
 * Правило: количество в каждой позиции строго положительное.
 */
public class QuantityValidator extends AbstractDocumentValidator {

    @Override
    protected void check(ErpDocument document, ValidationContext context) {
        // TODO:
        // 1. Переберите позиции документа.
        // 2. Если quantity <= 0 (BigDecimal#signum) — DocumentValidationException с кодом INVALID_QUANTITY.
        // 3. В сообщении укажите наименование товара и значение количества.
        throw new UnsupportedOperationException("TODO");
    }
}
