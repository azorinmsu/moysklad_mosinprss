package ru.moysklad.intern.documentpatterns.decorator;

import java.util.Objects;

/**
 * Базовый декоратор: хранит обёрнутую операцию. Наследники добавляют поведение до/после
 * {@code delegate.execute(...)} и обязаны делегировать основную операцию.
 */
public abstract class DocumentOperationDecorator implements DocumentOperation {

    protected final DocumentOperation delegate;

    protected DocumentOperationDecorator(DocumentOperation delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }
}
