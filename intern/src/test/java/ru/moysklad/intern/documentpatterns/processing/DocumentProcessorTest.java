package ru.moysklad.intern.documentpatterns.processing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.documentpatterns.factory.AcceptanceDocumentFactory;
import ru.moysklad.intern.documentpatterns.factory.ShipmentDocumentFactory;
import ru.moysklad.intern.documentpatterns.factory.TransferDocumentFactory;
import ru.moysklad.intern.documentpatterns.model.AcceptanceDocument;
import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentProcessingException;
import ru.moysklad.intern.documentpatterns.model.DocumentStatus;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.ShipmentDocument;
import ru.moysklad.intern.documentpatterns.model.StockMovement;
import ru.moysklad.intern.documentpatterns.model.TransferDocument;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventPublisher;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventType;
import ru.moysklad.intern.documentpatterns.repository.InMemoryStockRepository;
import ru.moysklad.intern.documentpatterns.repository.StockHistoryEntry;
import ru.moysklad.intern.documentpatterns.repository.StockKey;
import ru.moysklad.intern.documentpatterns.repository.StockRepository;
import ru.moysklad.intern.documentpatterns.strategy.AcceptanceMovementStrategy;
import ru.moysklad.intern.documentpatterns.strategy.StockMovementStrategy;
import ru.moysklad.intern.documentpatterns.support.FailingStockRepository;
import ru.moysklad.intern.documentpatterns.support.RecordingEventListener;
import ru.moysklad.intern.documentpatterns.support.TestStocks;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidationChain;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidator;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.acceptance;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.position;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.product;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.qty;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.shipment;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.transfer;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.warehouse;

/**
 * Часть 4. Template Method — общий алгоритм проведения.
 */
class DocumentProcessorTest {

    @Test
    @DisplayName("Шаги выполняются в порядке: цепочка → специфичная проверка → расчёт → применение → событие")
    void stepsAreExecutedInOrder() {
        List<String> steps = new ArrayList<>();
        DocumentValidator chain = (doc, ctx) -> steps.add("chain");
        StockRepository repo = new StockRepository() {
            @Override
            public BigDecimal getQuantity(StockKey key) {
                return BigDecimal.ZERO;
            }

            @Override
            public void apply(List<StockMovement> movements) {
                steps.add("apply");
            }

            @Override
            public List<StockHistoryEntry> history() {
                return List.of();
            }
        };
        DocumentEventPublisher publisher = new DocumentEventPublisher();
        publisher.subscribe(event -> steps.add("event:" + event.type() + ":" + event.documentStatus()));

        ProcessingInfrastructure infra = new ProcessingInfrastructure(chain, repo, publisher);
        AbstractDocumentProcessor processor = new AbstractDocumentProcessor(infra) {
            @Override
            protected void validateSpecific(ErpDocument document) {
                steps.add("validateSpecific");
            }

            @Override
            protected List<StockMovement> calculateMovements(ErpDocument document) {
                steps.add("calculateMovements");
                return List.of();
            }
        };
        AcceptanceDocument doc = acceptance(warehouse("A"), position(product("X"), "10"));
        processor.process(doc);

        assertThat(steps).containsExactly(
                "chain",
                "validateSpecific",
                "calculateMovements",
                "apply",
                "event:DOCUMENT_POSTED:POSTED"
        );
    }

    @Test
    @DisplayName("Успешное проведение приёмки: остаток вырос, статус POSTED, движения зафиксированы")
    void postsAcceptance() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        InMemoryStockRepository stocks = TestStocks.withQuantity(x, a, "20");
        DocumentEventPublisher publisher = new DocumentEventPublisher();
        RecordingEventListener listener = new RecordingEventListener();
        publisher.subscribe(listener);

        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                stocks,
                publisher
        );

        AcceptanceDocument doc = acceptance(a, position(x, "10"));
        new AcceptanceDocumentFactory().createProcessor(infra).process(doc);

        assertThat(stocks.getQuantity(StockKey.of(x, a))).isEqualByComparingTo(qty("30"));
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.POSTED);
        assertThat(doc.getPostedMovements()).hasSize(1);
        assertThat(doc.getPostedMovements().getFirst().delta()).isEqualByComparingTo(qty("10"));
        assertThat(listener.types()).containsExactly(DocumentEventType.DOCUMENT_POSTED);
    }

    @Test
    @DisplayName("Ошибка валидации: статус DRAFT, остатки не изменились, событие ошибки без события проведения")
    void validationErrorKeepsDocumentDraft() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        InMemoryStockRepository stocks = TestStocks.withQuantity(x, a, "20");
        var snapshotBefore = stocks.snapshot();
        DocumentEventPublisher publisher = new DocumentEventPublisher();
        RecordingEventListener listener = new RecordingEventListener();
        publisher.subscribe(listener);

        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                stocks,
                publisher
        );

        ShipmentDocument doc = shipment(a, position(x, "100"));
        ShipmentProcessor processor = (ShipmentProcessor) new ShipmentDocumentFactory().createProcessor(infra);

        assertThatThrownBy(() -> processor.process(doc))
                .isInstanceOf(DocumentProcessingException.class)
                .extracting(e -> ((DocumentProcessingException) e).getCode())
                .isEqualTo(DocumentErrorCode.INSUFFICIENT_STOCK);

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(stocks.snapshot()).isEqualTo(snapshotBefore);
        assertThat(listener.types()).containsExactly(DocumentEventType.DOCUMENT_PROCESSING_FAILED);
        assertThat(listener.events().getFirst().errorCode()).isEqualTo(DocumentErrorCode.INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("Ошибка применения движений: статус DRAFT, нет события проведения")
    void applyFailureKeepsDocumentDraft() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        InMemoryStockRepository stocks = TestStocks.withQuantity(x, a, "20");
        DocumentEventPublisher publisher = new DocumentEventPublisher();
        RecordingEventListener listener = new RecordingEventListener();
        publisher.subscribe(listener);

        FailingStockRepository failingRepo = new FailingStockRepository(
                stocks,
                () -> new DocumentProcessingException(DocumentErrorCode.INSUFFICIENT_STOCK, "Fail")
        );
        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                failingRepo,
                publisher
        );

        ShipmentDocument doc = shipment(a, position(x, "10"));
        ShipmentProcessor processor = (ShipmentProcessor) new ShipmentDocumentFactory().createProcessor(infra);

        assertThatThrownBy(() -> processor.process(doc))
                .isInstanceOf(DocumentProcessingException.class)
                .extracting(e -> ((DocumentProcessingException) e).getCode())
                .isEqualTo(DocumentErrorCode.INSUFFICIENT_STOCK);

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(doc.getPostedMovements()).isEmpty();
        assertThat(listener.types()).containsExactly(DocumentEventType.DOCUMENT_PROCESSING_FAILED);
    }

    @Test
    @DisplayName("Нет частичных изменений: перемещение при нехватке не меняет ни один склад")
    void noPartialChanges() {
        Warehouse a = warehouse("A");
        Warehouse b = warehouse("B");
        Product x = product("X");
        InMemoryStockRepository stocks = TestStocks.withQuantity(x, a, "2");

        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                (d, c) -> {},
                stocks,
                new DocumentEventPublisher()
        );

        TransferDocument doc = transfer(a, b, position(x, "5"));
        TransferProcessor processor = (TransferProcessor) new TransferDocumentFactory().createProcessor(infra);

        assertThatThrownBy(() -> processor.process(doc))
                .isInstanceOf(DocumentProcessingException.class);

        assertThat(stocks.getQuantity(StockKey.of(x, a))).isEqualByComparingTo(qty("2"));
        assertThat(stocks.getQuantity(StockKey.of(x, b))).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stocks.history()).isEmpty();
    }

    @Test
    @DisplayName("Повторное проведение → ALREADY_POSTED, остаток изменился один раз")
    void repeatedPostIsRejected() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        InMemoryStockRepository stocks = TestStocks.withQuantity(x, a, "20");
        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                stocks,
                new DocumentEventPublisher()
        );

        AcceptanceDocument doc = acceptance(a, position(x, "10"));
        AcceptanceProcessor processor = (AcceptanceProcessor) new AcceptanceDocumentFactory().createProcessor(infra);

        processor.process(doc);
        assertThat(stocks.getQuantity(StockKey.of(x, a))).isEqualByComparingTo(qty("30"));

        assertThatThrownBy(() -> processor.process(doc))
                .isInstanceOf(DocumentProcessingException.class)
                .extracting(e -> ((DocumentProcessingException) e).getCode())
                .isEqualTo(DocumentErrorCode.ALREADY_POSTED);

        assertThat(stocks.getQuantity(StockKey.of(x, a))).isEqualByComparingTo(qty("30"));
    }

    @Test
    @DisplayName("Специфичные правила типов: склад-источник у приёмки, дубли позиций в перемещении")
    void specificRules() {
        Warehouse a = warehouse("A");
        Warehouse b = warehouse("B");
        Product x = product("X");
        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                (d, c) -> {},
                TestStocks.empty(),
                new DocumentEventPublisher()
        );

        AcceptanceDocument acc = acceptance(a, position(x, "10"));
        acc.setSourceWarehouse(b);
        AcceptanceProcessor accProc = (AcceptanceProcessor) new AcceptanceDocumentFactory().createProcessor(infra);
        assertThatThrownBy(() -> accProc.process(acc))
                .isInstanceOf(DocumentProcessingException.class)
                .extracting(e -> ((DocumentProcessingException) e).getCode())
                .isEqualTo(DocumentErrorCode.UNEXPECTED_SOURCE_WAREHOUSE);

        ShipmentDocument ship = shipment(a, position(x, "10"));
        ship.setTargetWarehouse(b);
        ShipmentProcessor shipProc = (ShipmentProcessor) new ShipmentDocumentFactory().createProcessor(infra);
        assertThatThrownBy(() -> shipProc.process(ship))
                .isInstanceOf(DocumentProcessingException.class)
                .extracting(e -> ((DocumentProcessingException) e).getCode())
                .isEqualTo(DocumentErrorCode.UNEXPECTED_TARGET_WAREHOUSE);

        TransferDocument tr = transfer(a, b, position(x, "5"), position(x, "5"));
        TransferProcessor trProc = (TransferProcessor) new TransferDocumentFactory().createProcessor(infra);
        assertThatThrownBy(() -> trProc.process(tr))
                .isInstanceOf(DocumentProcessingException.class)
                .extracting(e -> ((DocumentProcessingException) e).getCode())
                .isEqualTo(DocumentErrorCode.DUPLICATE_POSITION);
    }

    @Test
    @DisplayName("null-документ → DOCUMENT_REQUIRED; стратегия вызывается один раз за проведение")
    void nullDocumentAndSingleCalculation() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        InMemoryStockRepository stocks = TestStocks.withQuantity(x, a, "20");
        AtomicInteger calcCount = new AtomicInteger();
        StockMovementStrategy countingStrategy = doc -> {
            calcCount.incrementAndGet();
            return new AcceptanceMovementStrategy().calculateMovements(doc);
        };

        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                stocks,
                new DocumentEventPublisher()
        );

        AcceptanceProcessor processor = new AcceptanceProcessor(infra, countingStrategy);

        assertThatThrownBy(() -> processor.process(null))
                .isInstanceOf(DocumentProcessingException.class)
                .extracting(e -> ((DocumentProcessingException) e).getCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_REQUIRED);

        AcceptanceDocument doc = acceptance(a, position(x, "10"));
        processor.process(doc);
        assertThat(calcCount.get()).isEqualTo(1);
    }
}
