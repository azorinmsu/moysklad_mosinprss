package ru.moysklad.intern.documentpatterns.factory;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.TransferDocument;
import ru.moysklad.intern.documentpatterns.processing.AbstractDocumentProcessor;
import ru.moysklad.intern.documentpatterns.processing.ProcessingInfrastructure;
import ru.moysklad.intern.documentpatterns.processing.TransferProcessor;
import ru.moysklad.intern.documentpatterns.strategy.StockMovementStrategy;
import ru.moysklad.intern.documentpatterns.strategy.TransferMovementStrategy;
import ru.moysklad.intern.entity.operation.DocumentsType;

import java.time.LocalDateTime;
import java.util.UUID;

public class TransferDocumentFactory extends DocumentFactory {

    @Override
    public DocumentsType documentType() {
        return DocumentsType.SHIFT;
    }

    @Override
    protected ErpDocument newDocument(UUID id, LocalDateTime moment) {
        return new TransferDocument(id, moment);
    }

    @Override
    public StockMovementStrategy createMovementStrategy() {
        return new TransferMovementStrategy();
    }

    @Override
    public AbstractDocumentProcessor createProcessor(ProcessingInfrastructure infrastructure) {
        return new TransferProcessor(infrastructure, createMovementStrategy());
    }
}
