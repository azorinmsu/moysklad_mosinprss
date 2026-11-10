package ru.moysklad.intern.documentpatterns.decorator;

import ru.moysklad.intern.documentpatterns.model.DocumentProcessingException;
import ru.moysklad.intern.documentpatterns.model.DocumentStatus;
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
        DocumentStatus status = document.getStatus();
        try {
            delegate.execute(document);
            auditTrail.record(new AuditRecord(
                    document.getId(),
                    document.getType(),
                    operationName,
                    status, // старый
                    document.getStatus(), // новый
                    AuditOutcome.SUCCESS,
                    null,
                    clock.instant()
            ));
        } catch (RuntimeException e) {
            auditTrail.record(new AuditRecord(
                    document.getId(),
                    document.getType(),
                    operationName,
                    status, // старый
                    document.getStatus(), // новый
                    AuditOutcome.FAILURE,
                    e instanceof DocumentProcessingException dpe ? dpe.getCode().name() : e.getClass().getSimpleName(),
                    clock.instant()
            ));
            throw e;
        }
    }
}
