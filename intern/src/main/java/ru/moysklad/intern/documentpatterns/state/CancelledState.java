package ru.moysklad.intern.documentpatterns.state;

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
        // TODO:
        // 1. В учебной модели CANCELLED — конечное состояние: повторно провести нельзя.
        // 2. Бросьте DocumentStateException с кодом ALREADY_CANCELLED.
        // (Повторное проведение после отмены — возможное расширение, см. ASSIGNMENT.md.)
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void unpost(DocumentContext context) {
        // TODO:
        // 1. Повторная отмена не должна менять остатки.
        // 2. Бросьте DocumentStateException с кодом ALREADY_CANCELLED.
        throw new UnsupportedOperationException("TODO");
    }
}
