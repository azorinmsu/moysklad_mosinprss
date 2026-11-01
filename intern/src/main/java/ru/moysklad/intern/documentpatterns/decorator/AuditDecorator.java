package ru.moysklad.intern.documentpatterns.decorator;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;

import java.time.Clock;
import java.util.Objects;

/**
 * Фиксирует в {@link AuditTrail} фактический результат каждой попытки операции.
 */
public class AuditDecorator extends DocumentOperationDecorator {

    private final AuditTrail auditTrail;
    private final String operationName;
    private final Clock clock;

    public AuditDecorator(DocumentOperation delegate, AuditTrail auditTrail, String operationName) {
        this(delegate, auditTrail, operationName, Clock.systemUTC());
    }

    public AuditDecorator(DocumentOperation delegate, AuditTrail auditTrail, String operationName, Clock clock) {
        super(delegate);
        this.auditTrail = Objects.requireNonNull(auditTrail, "auditTrail");
        this.operationName = Objects.requireNonNull(operationName, "operationName");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public void execute(ErpDocument document) {
        // TODO:
        // 1. Запомните статус документа до операции.
        // 2. Вызовите delegate.execute(document).
        // 3. Успех: запишите AuditRecord с outcome = SUCCESS и ФАКТИЧЕСКИМ статусом после операции.
        // 4. RuntimeException: запишите AuditRecord с outcome = FAILURE, statusAfter = фактический статус
        //    (он не должен стать POSTED), errorCode = код DocumentProcessingException
        //    или имя класса исключения; затем пробросьте исключение.
        // 5. Ошибочная операция не должна попасть в аудит как успешная.
        throw new UnsupportedOperationException("TODO");
    }
}
