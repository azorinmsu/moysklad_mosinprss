package ru.moysklad.intern.documentpatterns.command;

import ru.moysklad.intern.documentpatterns.model.DocumentProcessingException;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.documentpatterns.observer.DocumentEvent;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventPublisher;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventType;
import ru.moysklad.intern.documentpatterns.repository.StockRepository;
import ru.moysklad.intern.documentpatterns.state.DocumentContext;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Команда отмены проведения: сторнирует движения документа обратными движениями.
 *
 * <p>Это не удаление документа и не «откат» в смысле undo-стека: документ остаётся в системе со статусом
 * CANCELLED, а в журнале остатков появляются новые обратные движения.
 */
public class UnpostDocumentCommand implements DocumentCommand {

    private final UUID operationId;
    private final ErpDocument document;
    private final StockRepository stockRepository;
    private final DocumentEventPublisher eventPublisher;

    public UnpostDocumentCommand(UUID operationId, ErpDocument document,
                                 StockRepository stockRepository, DocumentEventPublisher eventPublisher) {
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.document = Objects.requireNonNull(document, "document");
        this.stockRepository = Objects.requireNonNull(stockRepository, "stockRepository");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    @Override
    public UUID operationId() {
        return operationId;
    }

    @Override
    public String name() {
        return "UNPOST";
    }

    @Override
    public ErpDocument document() {
        return document;
    }

    @Override
    public void execute() {
        DocumentContext ctx = new DocumentContext(document);
        try {
            ctx.unpost(); // проверяем

            List<StockMovement> reverses = document.getPostedMovements().stream()
                    .map(m -> new StockMovement(
                            m.documentId(), m.product(), m.warehouse(), m.lot(), m.delta().negate()))
                    .toList();

            stockRepository.apply(reverses); // перепроверка остатков

            ctx.commit();

            eventPublisher.publish(DocumentEvent.of(DocumentEventType.DOCUMENT_UNPOSTED, document));

        } catch (DocumentProcessingException e) {
            eventPublisher.publish(DocumentEvent.failed(document, e.getCode()));
            throw e;
        }
    }
}
