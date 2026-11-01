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
        // TODO:
        // 1. null-подписчик — IllegalArgumentException.
        // 2. Повторная подписка того же экземпляра не должна приводить к двойной доставке.
        // 3. Добавьте подписчика в listeners (коллекция потокобезопасна).
        throw new UnsupportedOperationException("TODO");
    }

    public void unsubscribe(DocumentEventListener listener) {
        // TODO: удалите подписчика; отписка неизвестного подписчика — не ошибка.
        throw new UnsupportedOperationException("TODO");
    }

    public void publish(DocumentEvent event) {
        // TODO:
        // 1. null-событие — IllegalArgumentException.
        // 2. Доставьте событие каждому подписчику в порядке подписки.
        // 3. Исключение одного подписчика перехватите, сохраните в deliveryFailures (DeliveryFailure)
        //    и продолжите доставку остальным.
        // 4. Метод не должен пробрасывать исключения подписчиков: операция уже зафиксирована.
        throw new UnsupportedOperationException("TODO");
    }

    public List<DocumentEventListener> listeners() {
        return List.copyOf(listeners);
    }

    public List<DeliveryFailure> deliveryFailures() {
        return List.copyOf(deliveryFailures);
    }
}
