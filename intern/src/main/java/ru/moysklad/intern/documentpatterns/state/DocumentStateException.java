package ru.moysklad.intern.documentpatterns.state;

import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentProcessingException;

/**
 * Недопустимый переход жизненного цикла (например, повторное проведение).
 */
public class DocumentStateException extends DocumentProcessingException {

    public DocumentStateException(DocumentErrorCode code, String message) {
        super(code, message);
    }
}
