package ru.moysklad.intern.documentpatterns.observer;

import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentStatus;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.entity.operation.DocumentsType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Событие жизненного цикла документа. Неизменяемо; {@code eventId} уникален для каждой публикации
 * и позволяет подписчикам отбрасывать повторную доставку того же события.
 *
 * @param errorCode код ошибки — только для {@link DocumentEventType#DOCUMENT_PROCESSING_FAILED}
 */
public record DocumentEvent(
        UUID eventId,
        DocumentEventType type,
        UUID documentId,
        DocumentsType documentType,
        DocumentStatus documentStatus,
        DocumentErrorCode errorCode,
        Instant occurredAt
) {

    public DocumentEvent {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    /** Событие с текущим статусом документа. */
    public static DocumentEvent of(DocumentEventType type, ErpDocument document) {
        return new DocumentEvent(UUID.randomUUID(), type, document.getId(), document.getType(),
                document.getStatus(), null, Instant.now());
    }

    public static DocumentEvent failed(ErpDocument document, DocumentErrorCode errorCode) {
        return new DocumentEvent(UUID.randomUUID(), DocumentEventType.DOCUMENT_PROCESSING_FAILED,
                document.getId(), document.getType(), document.getStatus(), errorCode, Instant.now());
    }
}
