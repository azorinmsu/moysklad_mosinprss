package ru.moysklad.intern.documentpatterns;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.documentpatterns.command.CommandOutcome;
import ru.moysklad.intern.documentpatterns.decorator.AuditOutcome;
import ru.moysklad.intern.documentpatterns.decorator.AuditRecord;
import ru.moysklad.intern.documentpatterns.decorator.AuditTrail;
import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentProcessingException;
import ru.moysklad.intern.documentpatterns.model.DocumentStatus;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.observer.DocumentEvent;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventPublisher;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventType;
import ru.moysklad.intern.documentpatterns.repository.InMemoryStockRepository;
import ru.moysklad.intern.documentpatterns.service.DocumentService;
import ru.moysklad.intern.documentpatterns.support.RecordingEventListener;
import ru.moysklad.intern.documentpatterns.support.TestStocks;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidationException;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.operation.DocumentsType;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.product;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.qty;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.warehouse;

/**
 * Часть 10. Сквозной сценарий через {@code DocumentService} без внешних сервисов.
 *
 * <pre>
 * Начальные остатки (TestStocks.workflowInitialStocks): A: X=20, Y=10; B: X=0, Y=0
 * 1. Приёмка 10 X на A           → A/X = 30
 * 2. Отгрузка 7 X с A            → A/X = 23
 * 3. Перемещение 5 X с A на B    → A/X = 18, B/X = 5
 * 4. Отмена перемещения          → A/X = 23, B/X = 0
 * 5. Отгрузка 100 X с A          → INSUFFICIENT_STOCK, остатки прежние, DRAFT, нет DOCUMENT_POSTED
 * </pre>
 */
class DocumentWorkflowTest {

    @Test
    @DisplayName("Сквозной сценарий: приёмка → отгрузка → перемещение → отмена → ошибка")
    void fullDocumentLifecycle() {
        Warehouse a = warehouse("A");
        Warehouse b = warehouse("B");
        Product x = product("X");
        Product y = product("Y");

        InMemoryStockRepository stocks = TestStocks.workflowInitialStocks(x, y, a, b);
        DocumentEventPublisher publisher = new DocumentEventPublisher();
        RecordingEventListener listener = new RecordingEventListener();
        publisher.subscribe(listener);

        AuditTrail auditTrail = new AuditTrail();
        List<String> log = new ArrayList<>();
        DocumentService service = DocumentService.withDefaultDocuments(stocks, publisher, auditTrail, log::add);

        // 1. Приёмка 10 X на A → A/X = 30
        ErpDocument acceptanceDoc = service.create(DocumentsType.GOODS_ACCEPTANCE, builder -> builder
                .id(UUID.randomUUID())
                .targetWarehouse(a)
                .addPosition(x, qty("10")));
        CommandOutcome outcome1 = service.post(acceptanceDoc);
        assertThat(outcome1).isEqualTo(CommandOutcome.EXECUTED);
        assertThat(stocks.getQuantity(x, a)).isEqualByComparingTo(qty("30"));
        assertThat(acceptanceDoc.getStatus()).isEqualTo(DocumentStatus.POSTED);

        // 2. Отгрузка 7 X с A → A/X = 23
        ErpDocument shipmentDoc = service.create(DocumentsType.SHIPMENT, builder -> builder
                .id(UUID.randomUUID())
                .sourceWarehouse(a)
                .addPosition(x, qty("7")));
        CommandOutcome outcome2 = service.post(shipmentDoc);
        assertThat(outcome2).isEqualTo(CommandOutcome.EXECUTED);
        assertThat(stocks.getQuantity(x, a)).isEqualByComparingTo(qty("23"));
        assertThat(shipmentDoc.getStatus()).isEqualTo(DocumentStatus.POSTED);

        // 3. Перемещение 5 X с A на B → A/X = 18, B/X = 5
        ErpDocument transferDoc = service.create(DocumentsType.SHIFT, builder -> builder
                .id(UUID.randomUUID())
                .sourceWarehouse(a)
                .targetWarehouse(b)
                .addPosition(x, qty("5")));
        CommandOutcome outcome3 = service.post(transferDoc);
        assertThat(outcome3).isEqualTo(CommandOutcome.EXECUTED);
        assertThat(stocks.getQuantity(x, a)).isEqualByComparingTo(qty("18"));
        assertThat(stocks.getQuantity(x, b)).isEqualByComparingTo(qty("5"));
        assertThat(transferDoc.getStatus()).isEqualTo(DocumentStatus.POSTED);

        // 4. Отмена перемещения → A/X = 23, B/X = 0
        CommandOutcome outcome4 = service.unpost(transferDoc);
        assertThat(outcome4).isEqualTo(CommandOutcome.EXECUTED);
        assertThat(stocks.getQuantity(x, a)).isEqualByComparingTo(qty("23"));
        assertThat(stocks.getQuantity(x, b)).isEqualByComparingTo(qty("0"));
        assertThat(transferDoc.getStatus()).isEqualTo(DocumentStatus.CANCELLED);

        // 5. Отгрузка 100 X с A → INSUFFICIENT_STOCK, snapshot() прежний, статус DRAFT,
        //  для этого документа нет DOCUMENT_POSTED, есть DOCUMENT_PROCESSING_FAILED.
        var snapshotBefore = stocks.snapshot();
        ErpDocument failShipmentDoc = service.create(DocumentsType.SHIPMENT, builder -> builder
                .id(UUID.randomUUID())
                .sourceWarehouse(a)
                .addPosition(x, qty("100")));

        assertThatThrownBy(() -> service.post(failShipmentDoc))
                .isInstanceOf(DocumentValidationException.class)
                .extracting(e -> ((DocumentValidationException) e).getCode())
                .isEqualTo(DocumentErrorCode.INSUFFICIENT_STOCK);

        assertThat(stocks.snapshot()).isEqualTo(snapshotBefore);
        assertThat(failShipmentDoc.getStatus()).isEqualTo(DocumentStatus.DRAFT);

        List<DocumentEvent> failDocEvents = listener.events().stream()
                .filter(e -> e.documentId().equals(failShipmentDoc.getId()))
                .toList();
        assertThat(failDocEvents.stream().map(DocumentEvent::type))
                .contains(DocumentEventType.DOCUMENT_CREATED, DocumentEventType.DOCUMENT_PROCESSING_FAILED)
                .doesNotContain(DocumentEventType.DOCUMENT_POSTED);

        // 4: Остаток Y не менялся на протяжении всего сценария
        assertThat(stocks.getQuantity(y, a)).isEqualByComparingTo(qty("10"));
        assertThat(stocks.getQuantity(y, b)).isEqualByComparingTo(qty("0"));
    }

    @Test
    @DisplayName("Нет двойного изменения остатков при повторе операции с тем же operationId")
    void noDoubleStockChange() {
        Warehouse a = warehouse("A");
        Product x = product("X");

        InMemoryStockRepository stocks = TestStocks.empty();
        DocumentEventPublisher publisher = new DocumentEventPublisher();
        RecordingEventListener listener = new RecordingEventListener();
        publisher.subscribe(listener);

        AuditTrail auditTrail = new AuditTrail();
        DocumentService service = DocumentService.withDefaultDocuments(stocks, publisher, auditTrail, msg -> {});

        ErpDocument doc = service.create(DocumentsType.GOODS_ACCEPTANCE, b -> b
                .id(UUID.randomUUID())
                .targetWarehouse(a)
                .addPosition(x, qty("10")));

        UUID opId = UUID.randomUUID();
        CommandOutcome outcome1 = service.post(doc, opId);
        CommandOutcome outcome2 = service.post(doc, opId);

        assertThat(outcome1).isEqualTo(CommandOutcome.EXECUTED);
        assertThat(outcome2).isEqualTo(CommandOutcome.SKIPPED_DUPLICATE);
        assertThat(stocks.getQuantity(x, a)).isEqualByComparingTo(qty("10"));
        assertThat(listener.count(DocumentEventType.DOCUMENT_POSTED)).isEqualTo(1);
    }

    @Test
    @DisplayName("Неподдерживаемый тип документа (MAKE) → UNSUPPORTED_DOCUMENT_TYPE")
    void unsupportedDocumentType() {
        InMemoryStockRepository stocks = TestStocks.empty();
        DocumentEventPublisher publisher = new DocumentEventPublisher();
        AuditTrail auditTrail = new AuditTrail();
        DocumentService service = DocumentService.withDefaultDocuments(stocks, publisher, auditTrail, msg -> {});

        assertThatThrownBy(() -> service.create(DocumentsType.MAKE, b -> {}))
                .isInstanceOf(DocumentProcessingException.class)
                .extracting(e -> ((DocumentProcessingException) e).getCode())
                .isEqualTo(DocumentErrorCode.UNSUPPORTED_DOCUMENT_TYPE);
    }

    @Test
    @DisplayName("Аудит, история операций и события отражают сценарий")
    void auditHistoryAndEvents() {
        Warehouse a = warehouse("A");
        Product x = product("X");

        InMemoryStockRepository stocks = TestStocks.empty();
        DocumentEventPublisher publisher = new DocumentEventPublisher();
        RecordingEventListener listener = new RecordingEventListener();
        publisher.subscribe(listener);

        AuditTrail auditTrail = new AuditTrail();
        List<String> logs = new ArrayList<>();
        DocumentService service = DocumentService.withDefaultDocuments(stocks, publisher, auditTrail, logs::add);

        ErpDocument doc = service.create(DocumentsType.GOODS_ACCEPTANCE, b -> b
                .id(UUID.randomUUID())
                .targetWarehouse(a)
                .addPosition(x, qty("10")));

        service.post(doc);
        service.unpost(doc);

        assertThat(listener.types()).containsExactly(
                DocumentEventType.DOCUMENT_CREATED,
                DocumentEventType.DOCUMENT_POSTED,
                DocumentEventType.DOCUMENT_UNPOSTED
        );

        assertThat(auditTrail.records()).isNotEmpty();
        AuditRecord firstRecord = auditTrail.records().get(0);
        assertThat(firstRecord.operation()).isEqualTo("POST");
        assertThat(firstRecord.outcome()).isEqualTo(AuditOutcome.SUCCESS);

        assertThat(service.operationHistory()).hasSize(2);
        assertThat(service.operationHistory().get(0).commandName()).isEqualTo("POST");
        assertThat(service.operationHistory().get(0).outcome()).isEqualTo(CommandOutcome.EXECUTED);
        assertThat(service.operationHistory().get(1).commandName()).isEqualTo("UNPOST");
        assertThat(service.operationHistory().get(1).outcome()).isEqualTo(CommandOutcome.EXECUTED);

        assertThat(logs).isNotEmpty();
    }
}
