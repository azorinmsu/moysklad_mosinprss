package ru.moysklad.intern.documentpatterns.model;

/**
 * Статус документа в учебной модели жизненного цикла.
 *
 * <pre>
 * DRAFT --post--> POSTED --unpost--> CANCELLED
 * </pre>
 *
 * {@code CANCELLED} — «проведение отменено»: движения документа сторнированы, сам документ сохранён.
 * Это не удаление документа и не бизнес-отмена заказа (см. lab/document-patterns/ARCHITECTURE.md).
 */
public enum DocumentStatus {
    DRAFT,
    POSTED,
    CANCELLED
}
