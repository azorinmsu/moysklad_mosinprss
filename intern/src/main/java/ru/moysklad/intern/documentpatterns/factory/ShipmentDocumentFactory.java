package ru.moysklad.intern.documentpatterns.factory;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.processing.AbstractDocumentProcessor;
import ru.moysklad.intern.documentpatterns.processing.ProcessingInfrastructure;
import ru.moysklad.intern.documentpatterns.strategy.StockMovementStrategy;
import ru.moysklad.intern.entity.operation.DocumentsType;

import java.time.LocalDateTime;
import java.util.UUID;

public class ShipmentDocumentFactory extends DocumentFactory {

    @Override
    public DocumentsType documentType() {
        return DocumentsType.SHIPMENT;
    }

    @Override
    protected ErpDocument newDocument(UUID id, LocalDateTime moment) {
        // TODO: создайте ShipmentDocument.
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public StockMovementStrategy createMovementStrategy() {
        // TODO: верните стратегию движений отгрузки.
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public AbstractDocumentProcessor createProcessor(ProcessingInfrastructure infrastructure) {
        // TODO: создайте ShipmentProcessor со стратегией из createMovementStrategy().
        throw new UnsupportedOperationException("TODO");
    }
}
