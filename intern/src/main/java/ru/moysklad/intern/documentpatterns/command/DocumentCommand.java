package ru.moysklad.intern.documentpatterns.command;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;

import java.util.UUID;

/**
 * Паттерн Command: бизнес-операция над документом как объект.
 *
 * <p>Команда несёт контекст выполнения (операция, документ, зависимости) и уникальный {@link #operationId()},
 * по которому исполнитель отличает повторную доставку той же операции от новой.
 */
public interface DocumentCommand {

    UUID operationId();

    /** Имя операции для истории, например {@code POST} / {@code UNPOST}. */
    String name();

    ErpDocument document();

    void execute();
}
