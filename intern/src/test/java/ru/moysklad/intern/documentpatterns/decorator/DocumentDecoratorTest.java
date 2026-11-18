package ru.moysklad.intern.documentpatterns.decorator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.documentpatterns.factory.ShipmentDocumentFactory;
import ru.moysklad.intern.documentpatterns.model.AcceptanceDocument;
import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentProcessingException;
import ru.moysklad.intern.documentpatterns.model.DocumentStatus;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.ShipmentDocument;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventPublisher;
import ru.moysklad.intern.documentpatterns.processing.ProcessingInfrastructure;
import ru.moysklad.intern.documentpatterns.processing.ShipmentProcessor;
import ru.moysklad.intern.documentpatterns.repository.InMemoryStockRepository;
import ru.moysklad.intern.documentpatterns.support.TestStocks;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidationChain;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.acceptance;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.position;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.product;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.shipment;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.warehouse;

/**
 * Часть 5. Decorator — дополнительные возможности обработки.
 * Для изоляции подставляйте базовую операцию-лямбду, например {@code doc -> doc.applyStatus(POSTED)}.
 */
class DocumentDecoratorTest {

    @Test
    @DisplayName("Декоратор выполняет базовую операцию ровно один раз")
    void delegatesToBaseOperation() {
        AtomicInteger counter = new AtomicInteger();
        DocumentOperation base = doc -> counter.incrementAndGet();
        AcceptanceDocument doc = acceptance(warehouse("A"), position(product("X"), "10"));

        new LoggingDecorator(base, s -> {}).execute(doc);
        assertThat(counter.get()).isEqualTo(1);

        counter.set(0);
        new AuditDecorator(base, new AuditTrail(), "TEST").execute(doc);
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Логирование: начало и успех, результат операции не меняется")
    void loggingWritesStartAndSuccess() {
        List<String> logs = new ArrayList<>();
        DocumentOperation base = doc -> doc.applyStatus(DocumentStatus.POSTED);
        AcceptanceDocument doc = acceptance(warehouse("A"), position(product("X"), "10"));

        new LoggingDecorator(base, logs::add).execute(doc);

        assertThat(logs).hasSize(2);
        assertThat(logs.get(1)).contains("POSTED");
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.POSTED);
    }

    @Test
    @DisplayName("Аудит успешной операции: SUCCESS, статус DRAFT → POSTED")
    void auditRecordsSuccess() {
        AuditTrail auditTrail = new AuditTrail();
        DocumentOperation base = doc -> doc.applyStatus(DocumentStatus.POSTED);
        AcceptanceDocument doc = acceptance(warehouse("A"), position(product("X"), "10"));

        new AuditDecorator(base, auditTrail, "POST").execute(doc);

        assertThat(auditTrail.records()).hasSize(1);
        AuditRecord record = auditTrail.records().getFirst();
        assertThat(record.operation()).isEqualTo("POST");
        assertThat(record.statusBefore()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(record.statusAfter()).isEqualTo(DocumentStatus.POSTED);
        assertThat(record.outcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(record.errorCode()).isNull();
    }

    @Test
    @DisplayName("Комбинация декораторов в любом порядке: одна запись аудита и строки лога")
    void decoratorsCombine() {
        // Audit(Logging(base))
        List<String> logs1 = new ArrayList<>();
        AuditTrail trail1 = new AuditTrail();
        DocumentOperation base1 = doc -> doc.applyStatus(DocumentStatus.POSTED);
        AcceptanceDocument doc1 = acceptance(warehouse("A"), position(product("X"), "10"));
        new AuditDecorator(new LoggingDecorator(base1, logs1::add), trail1, "POST").execute(doc1);
        assertThat(trail1.records()).hasSize(1);
        assertThat(logs1).hasSize(2);

        // Logging(Audit(base))
        List<String> logs2 = new ArrayList<>();
        AuditTrail trail2 = new AuditTrail();
        DocumentOperation base2 = doc -> doc.applyStatus(DocumentStatus.POSTED);
        AcceptanceDocument doc2 = acceptance(warehouse("A"), position(product("X"), "10"));
        new LoggingDecorator(new AuditDecorator(base2, trail2, "POST"), logs2::add).execute(doc2);
        assertThat(trail2.records()).hasSize(1);
        assertThat(logs2).hasSize(2);
    }

    @Test
    @DisplayName("Ошибка основной операции пробрасывается тем же исключением, в логе — ошибка")
    void failureIsPropagated() {
        List<String> logs = new ArrayList<>();
        DocumentProcessingException exception = new DocumentProcessingException(DocumentErrorCode.EMPTY_DOCUMENT, "empty");
        DocumentOperation base = doc -> { throw exception; };
        AcceptanceDocument doc = acceptance(warehouse("A"));

        LoggingDecorator decorator = new LoggingDecorator(base, logs::add);
        assertThatThrownBy(() -> decorator.execute(doc))
                .isSameAs(exception);

        assertThat(logs).anyMatch(l -> l.contains(DocumentErrorCode.EMPTY_DOCUMENT.name()));
    }

    @Test
    @DisplayName("Аудит ошибки: FAILURE с кодом, статус не стал POSTED")
    void auditRecordsFailure() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        InMemoryStockRepository stocks = TestStocks.withQuantity(x, a, "10");
        AuditTrail auditTrail = new AuditTrail();

        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                stocks,
                new DocumentEventPublisher()
        );
        ShipmentProcessor processor = (ShipmentProcessor) new ShipmentDocumentFactory().createProcessor(infra);
        AuditDecorator decorator = new AuditDecorator(processor, auditTrail, "POST");

        ShipmentDocument doc = shipment(a, position(x, "20"));

        assertThatThrownBy(() -> decorator.execute(doc))
                .isInstanceOf(DocumentProcessingException.class);

        assertThat(auditTrail.records()).hasSize(1);
        AuditRecord record = auditTrail.records().getFirst();
        assertThat(record.outcome()).isEqualTo(AuditOutcome.FAILURE);
        assertThat(record.errorCode()).isEqualTo(DocumentErrorCode.INSUFFICIENT_STOCK.name());
        assertThat(record.statusAfter()).isEqualTo(DocumentStatus.DRAFT);
    }

    @Test
    @DisplayName("Дополнительно: TimingDecorator измеряет длительность и при успехе, и при ошибке")
    void timingDecoratorMeasuresDuration() {
        long[] times = {100L, 350L, 500L, 800L};
        AtomicInteger idx = new AtomicInteger();
        LongSupplier nanoTime = () -> times[idx.getAndIncrement()];

        List<Duration> capturedDurations = new ArrayList<>();
        BiConsumer<ErpDocument, Duration> sink = (d, dur) -> capturedDurations.add(dur);

        DocumentOperation baseSuccess = doc -> {};
        AcceptanceDocument doc = acceptance(warehouse("A"));

        TimingDecorator decoratorSuccess = new TimingDecorator(baseSuccess, nanoTime, sink);
        decoratorSuccess.execute(doc);
        assertThat(capturedDurations).containsExactly(Duration.ofNanos(250L));

        DocumentOperation baseFail = d -> { throw new RuntimeException("error"); };
        TimingDecorator decoratorFail = new TimingDecorator(baseFail, nanoTime, sink);
        assertThatThrownBy(() -> decoratorFail.execute(doc))
                .isInstanceOf(RuntimeException.class);
        assertThat(capturedDurations).containsExactly(Duration.ofNanos(250L), Duration.ofNanos(300L));
    }
}
