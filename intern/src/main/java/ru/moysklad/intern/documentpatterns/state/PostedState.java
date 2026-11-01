package ru.moysklad.intern.documentpatterns.state;

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
        // TODO:
        // 1. Повторное проведение запрещено — иначе остатки изменятся дважды.
        // 2. Бросьте DocumentStateException с кодом ALREADY_POSTED.
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void unpost(DocumentContext context) {
        // TODO:
        // 1. Проведённый документ можно отменить: запланируйте переход в CANCELLED.
        // 2. Обратные движения строит не состояние, а UnpostDocumentCommand.
        throw new UnsupportedOperationException("TODO");
    }
}
