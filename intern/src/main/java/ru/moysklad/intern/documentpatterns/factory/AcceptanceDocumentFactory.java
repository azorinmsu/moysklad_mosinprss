package ru.moysklad.intern.documentpatterns.factory;

import ru.moysklad.intern.documentpatterns.model.AcceptanceDocument;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.processing.AbstractDocumentProcessor;
import ru.moysklad.intern.documentpatterns.processing.AcceptanceProcessor;
import ru.moysklad.intern.documentpatterns.processing.ProcessingInfrastructure;
import ru.moysklad.intern.documentpatterns.strategy.AcceptanceMovementStrategy;
import ru.moysklad.intern.documentpatterns.strategy.StockMovementStrategy;
import ru.moysklad.intern.entity.operation.DocumentsType;

import java.time.LocalDateTime;
import java.util.UUID;

public class AcceptanceDocumentFactory extends DocumentFactory {

    @Override
    public DocumentsType documentType() {
        return DocumentsType.GOODS_ACCEPTANCE;
    }

    @Override
    protected ErpDocument newDocument(UUID id, LocalDateTime moment) {
        return new AcceptanceDocument(id, moment);
    }

    @Override
    public StockMovementStrategy createMovementStrategy() {
        return new AcceptanceMovementStrategy();
    }

    @Override
    public AbstractDocumentProcessor createProcessor(ProcessingInfrastructure infrastructure) {
        return new AcceptanceProcessor(infrastructure, createMovementStrategy());
    }
}
