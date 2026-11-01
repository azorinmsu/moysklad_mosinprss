package ru.moysklad.intern.documentpatterns.model;

import ru.moysklad.intern.entity.operation.DocumentsType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Приёмка: приходует товар на склад-получатель.
 */
public final class AcceptanceDocument extends ErpDocument {

    public AcceptanceDocument(UUID id, LocalDateTime moment) {
        super(id, moment);
    }

    @Override
    public DocumentsType getType() {
        return DocumentsType.GOODS_ACCEPTANCE;
    }

    @Override
    public boolean requiresSourceWarehouse() {
        return false;
    }

    @Override
    public boolean requiresTargetWarehouse() {
        return true;
    }
}
