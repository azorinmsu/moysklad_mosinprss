package ru.moysklad.intern.documentpatterns.state;

import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
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
        context.transitionTo(DocumentStatus.POSTED);
    }

    @Override
    public void unpost(DocumentContext context) {
        throw new DocumentStateException(DocumentErrorCode.NOT_POSTED, "Черновик не может быть не проведенным");
    }
}
