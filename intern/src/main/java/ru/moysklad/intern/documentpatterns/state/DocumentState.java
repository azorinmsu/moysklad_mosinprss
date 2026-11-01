package ru.moysklad.intern.documentpatterns.state;

import ru.moysklad.intern.documentpatterns.model.DocumentStatus;

/**
 * Паттерн State: поведение документа зависит от его состояния.
 *
 * <p>Состояние решает, допустима ли операция, и <b>планирует</b> переход через
 * {@link DocumentContext#transitionTo(DocumentStatus)}. Сам статус документа меняется позже —
 * в {@link DocumentContext#commit()}, только после успешного выполнения операции.
 * Недопустимая операция — {@link DocumentStateException} с понятным кодом.
 *
 * <p>Реализации не хранят данных конкретного документа (всё — в контексте), поэтому они синглтоны.
 */
public interface DocumentState {

    /** Статус, которому соответствует состояние. */
    DocumentStatus status();

    /** Запрос на проведение документа. */
    void post(DocumentContext context);

    /** Запрос на отмену проведения документа. */
    void unpost(DocumentContext context);
}
