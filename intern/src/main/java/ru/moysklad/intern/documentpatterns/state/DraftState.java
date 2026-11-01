package ru.moysklad.intern.documentpatterns.state;

import ru.moysklad.intern.documentpatterns.model.DocumentStatus;

/**
 * Черновик: документ можно редактировать и провести.
 */
public final class DraftState implements DocumentState {

    public static final DraftState INSTANCE = new DraftState();

    private DraftState() {
    }

    @Override
    public DocumentStatus status() {
        return DocumentStatus.DRAFT;
    }

    @Override
    public void post(DocumentContext context) {
        // TODO:
        // 1. Черновик можно провести: запланируйте переход в POSTED через context.transitionTo(...).
        // 2. Не меняйте статус документа напрямую — его применит context.commit() после успешного проведения.
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void unpost(DocumentContext context) {
        // TODO:
        // 1. Черновик не проведён — отменять нечего.
        // 2. Бросьте DocumentStateException с кодом NOT_POSTED и понятным сообщением (id документа).
        throw new UnsupportedOperationException("TODO");
    }
}
