package ru.moysklad.intern.documentpatterns.model;

import ru.moysklad.intern.entity.operation.DocumentsType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Отгрузка: списывает товар со склада-источника.
 */
public final class ShipmentDocument extends ErpDocument {

    public ShipmentDocument(UUID id, LocalDateTime moment) {
        super(id, moment);
    }

    @Override
    public DocumentsType getType() {
        return DocumentsType.SHIPMENT;
    }

    @Override
    public boolean requiresSourceWarehouse() {
        return true;
    }

    @Override
    public boolean requiresTargetWarehouse() {
        return false;
    }
}
