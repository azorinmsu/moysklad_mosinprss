package ru.moysklad.intern.documentpatterns.observer;

/**
 * Подписчик на события документов (Observer).
 */
@FunctionalInterface
public interface DocumentEventListener {

    void onEvent(DocumentEvent event);
}
