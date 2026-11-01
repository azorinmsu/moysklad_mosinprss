package ru.moysklad.intern.documentpatterns.decorator;

import ru.moysklad.intern.documentpatterns.model.DocumentStatus;
import ru.moysklad.intern.entity.operation.DocumentsType;

import java.time.Instant;
import java.util.UUID;

/**
 * Запись аудита одной попытки выполнить операцию над документом.
 *
 * @param statusBefore статус до операции
 * @param statusAfter  фактический статус после операции (при ошибке совпадает со statusBefore)
 * @param errorCode    код ошибки ({@code DocumentErrorCode#name()} или имя класса исключения); при успехе {@code null}
 */
public record AuditRecord(
        UUID documentId,
        DocumentsType documentType,
        String operation,
        DocumentStatus statusBefore,
        DocumentStatus statusAfter,
        AuditOutcome outcome,
        String errorCode,
        Instant recordedAt
) {
}
