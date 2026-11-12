package ru.moysklad.intern.documentpatterns.command;

import ru.moysklad.intern.documentpatterns.model.DocumentProcessingException;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Исполнитель команд (Invoker): запускает команды, ведёт историю операций и не допускает
 * повторного выполнения одной и той же операции (идемпотентность по {@code operationId}).
 */
public class DocumentCommandExecutor {

    private final Set<UUID> completedOperations = ConcurrentHashMap.newKeySet();
    private final List<CommandHistoryEntry> history = new CopyOnWriteArrayList<>();
    private final Clock clock;

    public DocumentCommandExecutor() {
        this(Clock.systemUTC());
    }

    public DocumentCommandExecutor(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized CommandOutcome execute(DocumentCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Команда не может быть null");
        }
        if (completedOperations.contains(command.operationId())) {
            history.addLast(new CommandHistoryEntry(
                    command.operationId(),
                    command.name(),
                    command.document().getId(),
                    CommandOutcome.SKIPPED_DUPLICATE,
                    null,
                    clock.instant()
            ));
            return CommandOutcome.SKIPPED_DUPLICATE;
        }
        try {
            command.execute();
            completedOperations.add(command.operationId());
            history.addLast(new CommandHistoryEntry(
                    command.operationId(),
                    command.name(),
                    command.document().getId(),
                    CommandOutcome.EXECUTED,
                    null,
                    clock.instant()
            ));
            return CommandOutcome.EXECUTED;
        } catch (RuntimeException e) {
            history.addLast(new CommandHistoryEntry(
                    command.operationId(),
                    command.name(),
                    command.document().getId(),
                    CommandOutcome.FAILED,
                    e instanceof DocumentProcessingException dpe ? dpe.getCode().name() : e.getClass().getSimpleName(),
                    clock.instant()
            ));
            throw e;
        }
    }
    public List<CommandHistoryEntry> history() {
        return List.copyOf(history);
    }
}
