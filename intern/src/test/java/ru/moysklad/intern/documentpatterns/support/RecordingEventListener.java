package ru.moysklad.intern.documentpatterns.support;

import ru.moysklad.intern.documentpatterns.observer.DocumentEvent;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventListener;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventType;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Тестовый подписчик: запоминает все полученные события.
 */
public class RecordingEventListener implements DocumentEventListener {

    private final List<DocumentEvent> events = new CopyOnWriteArrayList<>();

    @Override
    public void onEvent(DocumentEvent event) {
        events.add(event);
    }

    public List<DocumentEvent> events() {
        return List.copyOf(events);
    }

    public List<DocumentEventType> types() {
        return events.stream().map(DocumentEvent::type).toList();
    }

    public long count(DocumentEventType type) {
        return events.stream().filter(event -> event.type() == type).count();
    }
}
