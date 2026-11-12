package ru.moysklad.intern.documentpatterns.factory;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.ShipmentDocument;
import ru.moysklad.intern.documentpatterns.processing.AbstractDocumentProcessor;
import ru.moysklad.intern.documentpatterns.processing.ProcessingInfrastructure;
import ru.moysklad.intern.documentpatterns.processing.ShipmentProcessor;
import ru.moysklad.intern.documentpatterns.strategy.ShipmentMovementStrategy;
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
        return new ShipmentDocument(id, moment);
    }

    @Override
    public StockMovementStrategy createMovementStrategy() {
        return new ShipmentMovementStrategy();
    }

    @Override
    public AbstractDocumentProcessor createProcessor(ProcessingInfrastructure infrastructure) {
        return new ShipmentProcessor(infrastructure, createMovementStrategy());
    }
}
