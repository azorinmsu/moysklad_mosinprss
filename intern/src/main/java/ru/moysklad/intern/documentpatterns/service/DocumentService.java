package ru.moysklad.intern.documentpatterns.service;

import ru.moysklad.intern.documentpatterns.builder.DocumentBuilder;
import ru.moysklad.intern.documentpatterns.command.CommandHistoryEntry;
import ru.moysklad.intern.documentpatterns.command.CommandOutcome;
import ru.moysklad.intern.documentpatterns.command.DocumentCommandExecutor;
import ru.moysklad.intern.documentpatterns.decorator.AuditTrail;
import ru.moysklad.intern.documentpatterns.decorator.DocumentOperation;
import ru.moysklad.intern.documentpatterns.factory.AcceptanceDocumentFactory;
import ru.moysklad.intern.documentpatterns.factory.DocumentFactory;
import ru.moysklad.intern.documentpatterns.factory.DocumentFactoryRegistry;
import ru.moysklad.intern.documentpatterns.factory.ShipmentDocumentFactory;
import ru.moysklad.intern.documentpatterns.factory.TransferDocumentFactory;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventPublisher;
import ru.moysklad.intern.documentpatterns.repository.StockRepository;
import ru.moysklad.intern.entity.operation.DocumentsType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        // TODO:
        // 1. Сохраните stockRepository, eventPublisher; создайте DocumentFactoryRegistry(factories)
        //    и DocumentCommandExecutor.
        // 2. Создайте ProcessingInfrastructure с DocumentValidationChain.defaultChain().
        // 3. Для каждой фабрики реестра один раз создайте обработчик (factory.createProcessor(...))
        //    и оберните его декораторами: new AuditDecorator(new LoggingDecorator(processor, log), auditTrail, "POST").
        // 4. Сохраните получившиеся операции в postingOperations по типу документа (EnumMap → Map.copyOf).
        throw new UnsupportedOperationException("TODO");
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
        // TODO:
        // 1. Найдите фабрику: factoryRegistry.forType(type).
        // 2. DocumentBuilder.forFactory(factory); передайте builder в specification; build().
        // 3. Опубликуйте DocumentEvent.of(DOCUMENT_CREATED, document) и верните документ.
        throw new UnsupportedOperationException("TODO");
    }

    public CommandOutcome post(ErpDocument document) {
        return post(document, UUID.randomUUID());
    }

    /** Проведение с явным {@code operationId}: повтор с тем же id не изменит остатки повторно. */
    public CommandOutcome post(ErpDocument document, UUID operationId) {
        // TODO:
        // 1. Найдите операцию проведения для document.getType() в postingOperations
        //    (нет — DocumentProcessingException UNSUPPORTED_DOCUMENT_TYPE).
        // 2. Создайте PostDocumentCommand(operationId, document, operation) и выполните через commandExecutor.
        // 3. Не вызывайте здесь валидацию, State или публикацию событий — это уже делает обработчик.
        throw new UnsupportedOperationException("TODO");
    }

    public CommandOutcome unpost(ErpDocument document) {
        return unpost(document, UUID.randomUUID());
    }

    public CommandOutcome unpost(ErpDocument document, UUID operationId) {
        // TODO: создайте UnpostDocumentCommand и выполните через commandExecutor.
        throw new UnsupportedOperationException("TODO");
    }

    public List<CommandHistoryEntry> operationHistory() {
        return commandExecutor.history();
    }
}
