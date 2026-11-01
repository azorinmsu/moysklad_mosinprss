package ru.moysklad.intern.documentpatterns.observer;

/**
 * Ошибка доставки события конкретному подписчику. Публикатор сохраняет её, но не пробрасывает:
 * сбой подписчика не должен делать успешную бизнес-операцию неуспешной.
 */
public record DeliveryFailure(DocumentEventListener listener, DocumentEvent event, RuntimeException error) {
}
