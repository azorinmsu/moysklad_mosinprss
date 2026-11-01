package ru.moysklad.intern.documentpatterns.model;

import java.util.Objects;

/**
 * Базовое бизнес-исключение обработки документа. Всегда содержит {@link DocumentErrorCode}.
 */
public class DocumentProcessingException extends RuntimeException {

    private final DocumentErrorCode code;

    public DocumentProcessingException(DocumentErrorCode code, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code");
    }

    public DocumentErrorCode getCode() {
        return code;
    }
}
