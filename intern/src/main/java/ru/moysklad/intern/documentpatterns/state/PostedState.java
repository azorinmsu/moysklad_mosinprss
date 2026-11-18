package ru.moysklad.intern.documentpatterns.state;

import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentStatus;

/**
 * Проведён: движения применены, документ нельзя редактировать и повторно проводить.
 */
public final class PostedState implements DocumentState {

    public static final PostedState INSTANCE = new PostedState();

    private PostedState() {
    }

    @Override
    public DocumentStatus status() {
        return DocumentStatus.POSTED;
    }

    @Override
    public void post(DocumentContext context) {
        throw new DocumentStateException(DocumentErrorCode.ALREADY_POSTED, "Документ уже проведен, повторное проведение невозможно");
    }

    @Override
    public void unpost(DocumentContext context) {
        context.transitionTo(DocumentStatus.CANCELLED);
    }
}
