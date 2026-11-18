package ru.moysklad.intern.documentpatterns.service;

import ru.moysklad.intern.documentpatterns.builder.DocumentBuilder;
import ru.moysklad.intern.documentpatterns.command.*;
import ru.moysklad.intern.documentpatterns.decorator.AuditDecorator;
import ru.moysklad.intern.documentpatterns.decorator.AuditTrail;
import ru.moysklad.intern.documentpatterns.decorator.DocumentOperation;
import ru.moysklad.intern.documentpatterns.decorator.LoggingDecorator;
import ru.moysklad.intern.documentpatterns.factory.AcceptanceDocumentFactory;
import ru.moysklad.intern.documentpatterns.factory.DocumentFactory;
import ru.moysklad.intern.documentpatterns.factory.DocumentFactoryRegistry;
import ru.moysklad.intern.documentpatterns.factory.ShipmentDocumentFactory;
import ru.moysklad.intern.documentpatterns.factory.TransferDocumentFactory;
import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentProcessingException;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.observer.DocumentEvent;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventPublisher;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventType;
import ru.moysklad.intern.documentpatterns.processing.AbstractDocumentProcessor;
import ru.moysklad.intern.documentpatterns.processing.ProcessingInfrastructure;
import ru.moysklad.intern.documentpatterns.repository.StockRepository;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidationChain;
import ru.moysklad.intern.entity.operation.DocumentsType;

import java.util.*;
import java.util.function.Consumer;

/**
 * Фасад лабораторной: объединяет все паттерны в один маршрут.
 *
 * <pre>
 * create:  Factory → Builder → событие DOCUMENT_CREATED
 * post:    Command → Executor → Decorator(аудит, лог) → Template Method
 *            [State → Chain → Strategy → атомарное применение → commit → Observer]
 * unpost:  Command → Executor → UnpostDocumentCommand
 *            [State → обратные движения → атомарное применение → commit → Observer]
 * </pre>
 *
 * <p>Сервис не содержит проверок, расчёта движений и {@code switch} по типу документа:
 * новый тип добавляется новой фабрикой в списке {@code factories}. Spring-бином не является.
 */
public class DocumentService {

    private final DocumentFactoryRegistry factoryRegistry;
    private final StockRepository stockRepository;
    private final DocumentEventPublisher eventPublisher;
    private final DocumentCommandExecutor commandExecutor;
    private final Map<DocumentsType, DocumentOperation> postingOperations;

    public DocumentService(List<? extends DocumentFactory> factories,
                           StockRepository stockRepository,
                           DocumentEventPublisher eventPublisher,
                           AuditTrail auditTrail,
                           Consumer<String> log) {
        this.stockRepository = Objects.requireNonNull(stockRepository, "stockRepository");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.factoryRegistry = new DocumentFactoryRegistry(factories);
        this.commandExecutor = new DocumentCommandExecutor();

        ProcessingInfrastructure infrastructure = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                stockRepository,
                eventPublisher
        );

        Map<DocumentsType, DocumentOperation> operations = new EnumMap<>(DocumentsType.class);
        for (DocumentFactory factory : factoryRegistry.factories()) {
            AbstractDocumentProcessor processor = factory.createProcessor(infrastructure);
            DocumentOperation decorated = new AuditDecorator(
                    new LoggingDecorator(processor, log),
                    auditTrail,
                    "POST"
            );
            operations.put(factory.documentType(), decorated);
        }
        this.postingOperations = Map.copyOf(operations);
    }

    /** Сервис со всеми обязательными типами документов лабораторной. */
    public static DocumentService withDefaultDocuments(StockRepository stockRepository,
                                                       DocumentEventPublisher eventPublisher,
                                                       AuditTrail auditTrail,
                                                       Consumer<String> log) {
        return new DocumentService(
                List.of(new AcceptanceDocumentFactory(), new ShipmentDocumentFactory(), new TransferDocumentFactory()),
                stockRepository, eventPublisher, auditTrail, log);
    }

    /**
     * Создаёт документ: фабрика нужного типа + builder для реквизитов и позиций.
     *
     * <pre>{@code
     * service.create(DocumentsType.GOODS_ACCEPTANCE, b -> b
     *         .id(id).targetWarehouse(a).addPosition(x, new BigDecimal("10")));
     * }</pre>
     */
    public ErpDocument create(DocumentsType type, Consumer<DocumentBuilder> specification) {
        DocumentFactory factory = factoryRegistry.forType(type);
        DocumentBuilder builder = DocumentBuilder.forFactory(factory);
        specification.accept(builder);

        ErpDocument doc = builder.build();

        eventPublisher.publish(DocumentEvent.of(DocumentEventType.DOCUMENT_CREATED, doc));
        return doc;
    }

    public CommandOutcome post(ErpDocument document) {
        return post(document, UUID.randomUUID());
    }

    /** Проведение с явным {@code operationId}: повтор с тем же id не изменит остатки повторно. */
    public CommandOutcome post(ErpDocument document, UUID operationId) {
        if (document == null)
            throw new DocumentProcessingException(DocumentErrorCode.DOCUMENT_REQUIRED, "Документ не передан");

        DocumentOperation operation = postingOperations.get(document.getType());
        if (operation == null)
            throw new DocumentProcessingException(
                    DocumentErrorCode.UNSUPPORTED_DOCUMENT_TYPE,
                    "Тип документа не поддерживается: " + document.getType()
            );
        PostDocumentCommand command = new PostDocumentCommand(operationId, document, operation);
        return commandExecutor.execute(command);
    }

    public CommandOutcome unpost(ErpDocument document) {
        return unpost(document, UUID.randomUUID());
    }

    public CommandOutcome unpost(ErpDocument document, UUID operationId) {
        if (document == null)
            throw new DocumentProcessingException(DocumentErrorCode.DOCUMENT_REQUIRED, "Документ не передан");

        UnpostDocumentCommand command = new UnpostDocumentCommand(
                operationId, document, stockRepository, eventPublisher
        );
        return commandExecutor.execute(command);
    }

    public List<CommandHistoryEntry> operationHistory() {
        return commandExecutor.history();
    }
}
