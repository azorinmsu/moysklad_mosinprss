package ru.moysklad.intern.documentpatterns.observer;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Журнал бизнес-событий документов (история операций для отчётов и разборов).
 *
 * <p>Отличие от {@code AuditDecorator}: декоратор фиксирует каждую <i>попытку</i> выполнения операции
 * (в том числе неуспешную) со статусом «до/после», а этот подписчик — опубликованные бизнес-события.
 */
public class AuditDocumentListener implements DocumentEventListener {

    private final List<DocumentEvent> journal = new CopyOnWriteArrayList<>();
    private final Set<UUID> seenEventIds = ConcurrentHashMap.newKeySet();

    @Override
    public void onEvent(DocumentEvent event) {
        if (event != null && seenEventIds.add(event.eventId())) {
            journal.add(event);
        }
    }

    public List<DocumentEvent> journal() {
        return List.copyOf(journal);
    }
}
