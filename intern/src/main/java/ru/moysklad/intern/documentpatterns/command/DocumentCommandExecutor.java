package ru.moysklad.intern.documentpatterns.command;

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

    public CommandOutcome execute(DocumentCommand command) {
        // TODO:
        // 1. null-команда — IllegalArgumentException.
        // 2. Если command.operationId() уже есть в completedOperations — НЕ выполняйте команду,
        //    запишите в history SKIPPED_DUPLICATE и верните SKIPPED_DUPLICATE.
        // 3. Иначе выполните command.execute():
        //    - успех: добавьте operationId в completedOperations, запишите EXECUTED, верните EXECUTED;
        //    - RuntimeException: запишите FAILED с кодом ошибки (DocumentProcessingException#getCode
        //      или имя класса) и пробросьте исключение. Неуспешную операцию можно повторить.
        // 4. Проверка «уже выполнено» и выполнение должны быть атомарны относительно других потоков
        //    (например, synchronized): иначе два потока выполнят одну операцию дважды.
        throw new UnsupportedOperationException("TODO");
    }

    public List<CommandHistoryEntry> history() {
        return List.copyOf(history);
    }
}
