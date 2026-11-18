package ru.moysklad.intern.documentpatterns.validation;

import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentPosition;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;

import java.math.BigDecimal;

/**
 * Правило: количество в каждой позиции строго положительное.
 */
public class QuantityValidator extends AbstractDocumentValidator {

    @Override
    protected void check(ErpDocument document, ValidationContext context) {
        for (DocumentPosition pos : document.getPositions()) {
            if (pos.quantity().compareTo(BigDecimal.ZERO) <= 0)
                throw new DocumentValidationException(
                        DocumentErrorCode.INVALID_QUANTITY,
                        String.format(
                                "Количество товара [%s] меньше или равно нулю: %s",
                                pos.product().getName(),
                                pos.quantity()
                        )
                );
        }
    }
}
