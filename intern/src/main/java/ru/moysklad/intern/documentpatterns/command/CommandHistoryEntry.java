package ru.moysklad.intern.documentpatterns.command;

import java.time.Instant;
import java.util.UUID;

/**
 * Запись истории операций исполнителя команд.
 *
 * @param errorCode код ошибки для {@link CommandOutcome#FAILED}, иначе {@code null}
 */
public record CommandHistoryEntry(
        UUID operationId,
        String commandName,
        UUID documentId,
        CommandOutcome outcome,
        String errorCode,
        Instant executedAt
) {
}
