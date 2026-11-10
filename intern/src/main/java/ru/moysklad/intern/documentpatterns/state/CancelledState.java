package ru.moysklad.intern.documentpatterns.state;

import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentStatus;

/**
 * Проведение отменено: движения сторнированы. В учебной модели — конечное состояние.
 */
public final class CancelledState implements DocumentState {

    public static final CancelledState INSTANCE = new CancelledState();

    private CancelledState() {
    }

    @Override
    public DocumentStatus status() {
        return DocumentStatus.CANCELLED;
    }

    @Override
    public void post(DocumentContext context) {
        throw new DocumentStateException(DocumentErrorCode.ALREADY_CANCELLED, "Документ уже отменен");
    }

    @Override
    public void unpost(DocumentContext context) {
        throw new DocumentStateException(DocumentErrorCode.ALREADY_CANCELLED, "Документ уже отменен");
    }
}
