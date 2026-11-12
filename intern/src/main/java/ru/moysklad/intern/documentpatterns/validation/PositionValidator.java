package ru.moysklad.intern.documentpatterns.validation;

import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;

/**
 * Правило: в документе есть хотя бы одна товарная позиция.
 */
public class PositionValidator extends AbstractDocumentValidator {

    @Override
    protected void check(ErpDocument document, ValidationContext context) {
        if (document.getPositions() == null || document.getPositions().isEmpty())
            throw new DocumentValidationException(
                    DocumentErrorCode.EMPTY_DOCUMENT,
                    "Отсутствуют товарные позиции документа с ID: " + document.getId()
            );
    }
}
