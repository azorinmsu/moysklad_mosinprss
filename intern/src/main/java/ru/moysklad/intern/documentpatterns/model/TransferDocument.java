package ru.moysklad.intern.documentpatterns.model;

import ru.moysklad.intern.entity.operation.DocumentsType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Перемещение ({@link DocumentsType#SHIFT}): списывает товар со склада-источника и приходует на склад-получатель.
 */
public final class TransferDocument extends ErpDocument {

    public TransferDocument(UUID id, LocalDateTime moment) {
        super(id, moment);
    }

    @Override
    public DocumentsType getType() {
        return DocumentsType.SHIFT;
    }

    @Override
    public boolean requiresSourceWarehouse() {
        return true;
    }

    @Override
    public boolean requiresTargetWarehouse() {
        return true;
    }
}
