package ru.moysklad.intern.documentpatterns.command;

public enum CommandOutcome {
    /** Команда выполнена. */
    EXECUTED,
    /** Операция с таким operationId уже успешно выполнена — повтор пропущен, остатки не изменены. */
    SKIPPED_DUPLICATE,
    /** Команда завершилась ошибкой (в историю; наружу пробрасывается исключение). */
    FAILED
}
