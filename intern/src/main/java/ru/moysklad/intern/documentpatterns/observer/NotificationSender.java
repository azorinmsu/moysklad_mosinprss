package ru.moysklad.intern.documentpatterns.observer;

/**
 * Канал отправки уведомлений (почта, мессенджер, push). В тестах — лямбда, собирающая сообщения.
 */
@FunctionalInterface
public interface NotificationSender {

    void send(String message);
}
