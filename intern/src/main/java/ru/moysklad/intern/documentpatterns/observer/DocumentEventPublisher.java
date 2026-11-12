package ru.moysklad.intern.documentpatterns.observer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Синхронный in-memory публикатор событий (Subject паттерна Observer).
 *
 * <p>Порядок для учебного режима: событие публикуется <b>после</b> фиксации результата операции
 * (остатки применены, статус изменён). Для реальной БД синхронный Observer не даёт гарантии доставки —
 * используют transactional outbox (см. ARCHITECTURE.md).
 */
public class DocumentEventPublisher {

    private final List<DocumentEventListener> listeners = new CopyOnWriteArrayList<>();
    private final List<DeliveryFailure> deliveryFailures = new CopyOnWriteArrayList<>();

    public void subscribe(DocumentEventListener listener) {
        if (listener == null)
            throw new IllegalArgumentException("Отсутствует подписчик - null");

        if (!listeners.contains(listener))
            listeners.add(listener);
    }

    public void unsubscribe(DocumentEventListener listener) {
        if (listener != null && listeners.contains(listener))
            listeners.remove(listener);
    }

    public void publish(DocumentEvent event) {
        if (event == null)
            throw new IllegalArgumentException("Событие не может быть null");

        for (DocumentEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException e) {
                deliveryFailures.add(new DeliveryFailure(listener, event, e));
            }
        }
    }

    public List<DocumentEventListener> listeners() {
        return List.copyOf(listeners);
    }

    public List<DeliveryFailure> deliveryFailures() {
        return List.copyOf(deliveryFailures);
    }
}
