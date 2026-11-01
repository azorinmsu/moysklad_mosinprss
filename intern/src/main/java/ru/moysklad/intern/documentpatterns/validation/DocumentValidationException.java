package ru.moysklad.intern.documentpatterns.validation;

import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentProcessingException;

/**
 * Нарушено бизнес-правило документа. Бросается звеном цепочки и прерывает её.
 */
public class DocumentValidationException extends DocumentProcessingException {

    public DocumentValidationException(DocumentErrorCode code, String message) {
        super(code, message);
    }
}
