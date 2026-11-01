package ru.moysklad.intern.documentpatterns.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Журнал бизнес-событий документов (история операций для отчётов и разборов).
 *
 * <p>Отличие от {@code AuditDecorator}: декоратор фиксирует каждую <i>попытку</i> выполнения операции
 * (в том числе неуспешную) со статусом «до/после», а этот подписчик — опубликованные бизнес-события.
 */
public class AuditDocumentListener implements DocumentEventListener {

    private final List<DocumentEvent> journal = new CopyOnWriteArrayList<>();

    @Override
    public void onEvent(DocumentEvent event) {
        // TODO:
        // 1. Добавьте событие в journal.
        // 2. Повторная доставка того же события (тот же eventId) не должна создавать дубль записи.
        throw new UnsupportedOperationException("TODO");
    }

    public List<DocumentEvent> journal() {
        return List.copyOf(journal);
    }
}
