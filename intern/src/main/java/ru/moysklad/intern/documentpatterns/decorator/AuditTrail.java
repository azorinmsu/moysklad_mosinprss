package ru.moysklad.intern.documentpatterns.decorator;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Потокобезопасный in-memory журнал аудита. <b>Готовая инфраструктура.</b>
 */
public class AuditTrail {

    private final List<AuditRecord> records = new CopyOnWriteArrayList<>();

    public void record(AuditRecord record) {
        records.add(Objects.requireNonNull(record, "record"));
    }

    public List<AuditRecord> records() {
        return List.copyOf(records);
    }

    public List<AuditRecord> records(UUID documentId) {
        return records.stream()
                .filter(record -> Objects.equals(documentId, record.documentId()))
                .toList();
    }
}
