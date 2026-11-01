package ru.moysklad.intern.documentpatterns.command;

import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventPublisher;
import ru.moysklad.intern.documentpatterns.repository.StockRepository;

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
        // TODO:
        // 1. Проверить возможность отмены: new DocumentContext(document).unpost()
        //    (State бросит NOT_POSTED / ALREADY_CANCELLED и запланирует переход в CANCELLED).
        // 2. Построить обратные движения из document.getPostedMovements():
        //    тот же документ, товар, склад, партия, delta.negate().
        // 3. Применить их атомарно: stockRepository.apply(...). Хранилище учтёт ТЕКУЩИЕ остатки:
        //    если со склада-получателя товар уже ушёл, отмена невозможна (INSUFFICIENT_STOCK).
        // 4. Изменить статус: lifecycle.commit() — только после успешного шага 3.
        // 5. Опубликовать DocumentEvent.of(DOCUMENT_UNPOSTED, document).
        // При DocumentProcessingException опубликуйте DocumentEvent.failed(...) и пробросьте исключение;
        // статус останется POSTED, остатки не изменятся.
        throw new UnsupportedOperationException("TODO");
    }
}
