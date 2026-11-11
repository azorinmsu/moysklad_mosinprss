package ru.moysklad.intern.documentpatterns.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.documentpatterns.factory.AcceptanceDocumentFactory;
import ru.moysklad.intern.documentpatterns.factory.ShipmentDocumentFactory;
import ru.moysklad.intern.documentpatterns.factory.TransferDocumentFactory;
import ru.moysklad.intern.documentpatterns.model.AcceptanceDocument;
import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentProcessingException;
import ru.moysklad.intern.documentpatterns.model.DocumentStatus;
import ru.moysklad.intern.documentpatterns.model.ShipmentDocument;
import ru.moysklad.intern.documentpatterns.model.TransferDocument;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventPublisher;
import ru.moysklad.intern.documentpatterns.processing.AcceptanceProcessor;
import ru.moysklad.intern.documentpatterns.processing.ProcessingInfrastructure;
import ru.moysklad.intern.documentpatterns.processing.ShipmentProcessor;
import ru.moysklad.intern.documentpatterns.processing.TransferProcessor;
import ru.moysklad.intern.documentpatterns.repository.InMemoryStockRepository;
import ru.moysklad.intern.documentpatterns.repository.StockHistoryEntry;
import ru.moysklad.intern.documentpatterns.repository.StockKey;
import ru.moysklad.intern.documentpatterns.support.TestStocks;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidationChain;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
 * Часть 6. Command — проведение и отмена проведения.
 */
class DocumentCommandTest {

    @Test
    @DisplayName("Проведение командой: EXECUTED, остаток изменился, запись в истории")
    void postCommand() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        InMemoryStockRepository stocks = TestStocks.withQuantity(x, a, "20");
        DocumentEventPublisher publisher = new DocumentEventPublisher();
        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                stocks,
                publisher
        );
        AcceptanceProcessor processor = (AcceptanceProcessor) new AcceptanceDocumentFactory().createProcessor(infra);
        AcceptanceDocument doc = acceptance(a, position(x, "10"));
        UUID opId = UUID.randomUUID();
        PostDocumentCommand command = new PostDocumentCommand(opId, doc, processor);
        DocumentCommandExecutor executor = new DocumentCommandExecutor();

        CommandOutcome outcome = executor.execute(command);
        assertThat(outcome).isEqualTo(CommandOutcome.EXECUTED);
        assertThat(stocks.getQuantity(StockKey.of(x, a))).isEqualByComparingTo(qty("30"));
        assertThat(executor.history()).hasSize(1);
        assertThat(executor.history().getFirst().outcome()).isEqualTo(CommandOutcome.EXECUTED);
    }

    @Test
    @DisplayName("Отмена проведения: обратные движения восстанавливают остатки, статус CANCELLED")
    void unpostCommand() {
        Warehouse a = warehouse("A");
        Warehouse b = warehouse("B");
        Product x = product("X");
        InMemoryStockRepository stocks = TestStocks.withQuantity(x, a, "20");
        DocumentEventPublisher publisher = new DocumentEventPublisher();
        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                stocks,
                publisher
        );
        TransferProcessor processor = (TransferProcessor) new TransferDocumentFactory().createProcessor(infra);
        TransferDocument doc = transfer(a, b, position(x, "5"));
        processor.process(doc);
        assertThat(stocks.getQuantity(StockKey.of(x, a))).isEqualByComparingTo(qty("15"));
        assertThat(stocks.getQuantity(StockKey.of(x, b))).isEqualByComparingTo(qty("5"));

        UnpostDocumentCommand unpost = new UnpostDocumentCommand(UUID.randomUUID(), doc, stocks, publisher);
        unpost.execute();

        assertThat(stocks.getQuantity(StockKey.of(x, a))).isEqualByComparingTo(qty("20"));
        assertThat(stocks.getQuantity(StockKey.of(x, b))).isEqualByComparingTo(qty("0"));
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.CANCELLED);
    }

    @Test
    @DisplayName("Повторное выполнение той же команды пропускается, новая команда проведения → ALREADY_POSTED")
    void repeatedPost() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        InMemoryStockRepository stocks = TestStocks.withQuantity(x, a, "20");
        DocumentEventPublisher publisher = new DocumentEventPublisher();
        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                stocks,
                publisher
        );
        AcceptanceProcessor processor = (AcceptanceProcessor) new AcceptanceDocumentFactory().createProcessor(infra);
        AcceptanceDocument doc = acceptance(a, position(x, "10"));
        UUID opId = UUID.randomUUID();
        PostDocumentCommand command = new PostDocumentCommand(opId, doc, processor);
        DocumentCommandExecutor executor = new DocumentCommandExecutor();

        CommandOutcome first = executor.execute(command);
        CommandOutcome second = executor.execute(command);
        assertThat(first).isEqualTo(CommandOutcome.EXECUTED);
        assertThat(second).isEqualTo(CommandOutcome.SKIPPED_DUPLICATE);
        assertThat(stocks.getQuantity(StockKey.of(x, a))).isEqualByComparingTo(qty("30"));

        PostDocumentCommand newCommand = new PostDocumentCommand(UUID.randomUUID(), doc, processor);
        assertThatThrownBy(() -> executor.execute(newCommand))
                .isInstanceOf(DocumentProcessingException.class)
                .extracting(e -> ((DocumentProcessingException) e).getCode())
                .isEqualTo(DocumentErrorCode.ALREADY_POSTED);

        assertThat(executor.history().getLast().outcome()).isEqualTo(CommandOutcome.FAILED);
    }

    @Test
    @DisplayName("Повторная отмена → ALREADY_CANCELLED, остатки не меняются")
    void repeatedUnpost() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        InMemoryStockRepository stocks = TestStocks.withQuantity(x, a, "20");
        DocumentEventPublisher publisher = new DocumentEventPublisher();
        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                stocks,
                publisher
        );
        AcceptanceProcessor processor = (AcceptanceProcessor) new AcceptanceDocumentFactory().createProcessor(infra);
        AcceptanceDocument doc = acceptance(a, position(x, "10"));
        processor.process(doc);

        DocumentCommandExecutor executor = new DocumentCommandExecutor();
        UnpostDocumentCommand unpost1 = new UnpostDocumentCommand(UUID.randomUUID(), doc, stocks, publisher);
        executor.execute(unpost1);
        var snapshot = stocks.snapshot();

        UnpostDocumentCommand unpost2 = new UnpostDocumentCommand(UUID.randomUUID(), doc, stocks, publisher);
        assertThatThrownBy(() -> executor.execute(unpost2))
                .isInstanceOf(DocumentProcessingException.class)
                .extracting(e -> ((DocumentProcessingException) e).getCode())
                .isEqualTo(DocumentErrorCode.ALREADY_CANCELLED);

        assertThat(stocks.snapshot()).isEqualTo(snapshot);
    }

    @Test
    @DisplayName("Отмена черновика → NOT_POSTED")
    void unpostDraftIsRejected() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        AcceptanceDocument doc = acceptance(a, position(x, "10"));
        UnpostDocumentCommand unpost = new UnpostDocumentCommand(UUID.randomUUID(), doc, TestStocks.empty(), new DocumentEventPublisher());

        assertThatThrownBy(unpost::execute)
                .isInstanceOf(DocumentProcessingException.class)
                .extracting(e -> ((DocumentProcessingException) e).getCode())
                .isEqualTo(DocumentErrorCode.NOT_POSTED);
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.DRAFT);
    }

    @Test
    @DisplayName("Отмена при недостаточном остатке: товар уже ушёл со склада-получателя")
    void unpostWithInsufficientStock() {
        Warehouse a = warehouse("A");
        Warehouse b = warehouse("B");
        Product x = product("X");
        InMemoryStockRepository stocks = TestStocks.withQuantity(x, a, "20");
        DocumentEventPublisher publisher = new DocumentEventPublisher();
        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                stocks,
                publisher
        );
        TransferProcessor transferProc = (TransferProcessor) new TransferDocumentFactory().createProcessor(infra);
        ShipmentProcessor shipmentProc = (ShipmentProcessor) new ShipmentDocumentFactory().createProcessor(infra);

        TransferDocument transferDoc = transfer(a, b, position(x, "5"));
        transferProc.process(transferDoc);

        ShipmentDocument shipmentDoc = shipment(b, position(x, "5"));
        shipmentProc.process(shipmentDoc);

        var snapshotBefore = stocks.snapshot();

        UnpostDocumentCommand unpost = new UnpostDocumentCommand(UUID.randomUUID(), transferDoc, stocks, publisher);
        assertThatThrownBy(unpost::execute)
                .isInstanceOf(DocumentProcessingException.class)
                .extracting(e -> ((DocumentProcessingException) e).getCode())
                .isEqualTo(DocumentErrorCode.INSUFFICIENT_STOCK);

        assertThat(transferDoc.getStatus()).isEqualTo(DocumentStatus.POSTED);
        assertThat(stocks.snapshot()).isEqualTo(snapshotBefore);
    }

    @Test
    @DisplayName("История: журнал остатков хранит прямые и обратные движения, исполнитель — операции")
    void historyIsPreserved() {
        Warehouse a = warehouse("A");
        Warehouse b = warehouse("B");
        Product x = product("X");
        InMemoryStockRepository stocks = TestStocks.withQuantity(x, a, "20");
        DocumentEventPublisher publisher = new DocumentEventPublisher();
        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                stocks,
                publisher
        );
        TransferProcessor processor = (TransferProcessor) new TransferDocumentFactory().createProcessor(infra);
        TransferDocument doc = transfer(a, b, position(x, "5"));
        DocumentCommandExecutor executor = new DocumentCommandExecutor();

        executor.execute(new PostDocumentCommand(UUID.randomUUID(), doc, processor));
        executor.execute(new UnpostDocumentCommand(UUID.randomUUID(), doc, stocks, publisher));

        assertThat(stocks.history(doc.getId())).hasSize(4);
        List<BigDecimal> deltas = stocks.history(doc.getId()).stream().map(entry -> entry.movement().delta()).toList();
        assertThat(deltas).containsExactly(qty("-5"), qty("5"), qty("5"), qty("-5"));

        assertThat(executor.history()).hasSize(2);
        assertThat(executor.history().get(0).commandName()).isEqualTo("POST");
        assertThat(executor.history().get(0).outcome()).isEqualTo(CommandOutcome.EXECUTED);
        assertThat(executor.history().get(1).commandName()).isEqualTo("UNPOST");
        assertThat(executor.history().get(1).outcome()).isEqualTo(CommandOutcome.EXECUTED);
    }

    @Test
    @DisplayName("Одна операция из нескольких потоков выполняется ровно один раз")
    void concurrentDuplicateIsExecutedOnce() throws Exception {
        Warehouse a = warehouse("A");
        Product x = product("X");
        InMemoryStockRepository stocks = TestStocks.withQuantity(x, a, "20");
        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                stocks,
                new DocumentEventPublisher()
        );
        AcceptanceProcessor processor = (AcceptanceProcessor) new AcceptanceDocumentFactory().createProcessor(infra);
        AcceptanceDocument doc = acceptance(a, position(x, "10"));
        UUID opId = UUID.randomUUID();
        PostDocumentCommand command = new PostDocumentCommand(opId, doc, processor);
        DocumentCommandExecutor executor = new DocumentCommandExecutor();

        int threadCount = 8;
        ExecutorService es = Executors.newFixedThreadPool(threadCount);
        List<Future<CommandOutcome>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(es.submit(() -> executor.execute(command)));
        }
        es.shutdown();
        boolean finished = es.awaitTermination(5, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        List<CommandOutcome> outcomes = new ArrayList<>();
        for (Future<CommandOutcome> f : futures) {
            outcomes.add(f.get());
        }

        assertThat(outcomes.stream().filter(o -> o == CommandOutcome.EXECUTED).count()).isEqualTo(1);
        assertThat(outcomes.stream().filter(o -> o == CommandOutcome.SKIPPED_DUPLICATE).count()).isEqualTo(threadCount - 1);
        assertThat(stocks.getQuantity(StockKey.of(x, a))).isEqualByComparingTo(qty("30"));
    }
}
