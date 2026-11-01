package ru.moysklad.intern.documentpatterns.validation;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;

/**
 * Правило: в документе есть хотя бы одна товарная позиция.
 */
public class PositionValidator extends AbstractDocumentValidator {

    @Override
    protected void check(ErpDocument document, ValidationContext context) {
        // TODO:
        // 1. Если document.getPositions() пуст — DocumentValidationException с кодом EMPTY_DOCUMENT.
        // 2. В сообщение добавьте id документа.
        throw new UnsupportedOperationException("TODO");
    }
}
