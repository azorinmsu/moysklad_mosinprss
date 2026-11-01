package ru.moysklad.intern.documentpatterns;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.documentpatterns.decorator.AuditOutcome;
import ru.moysklad.intern.documentpatterns.decorator.AuditRecord;
import ru.moysklad.intern.documentpatterns.decorator.AuditTrail;
import ru.moysklad.intern.documentpatterns.factory.AcceptanceDocumentFactory;
import ru.moysklad.intern.documentpatterns.factory.DocumentFactoryRegistry;
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
import ru.moysklad.intern.documentpatterns.observer.DocumentEvent;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventType;
import ru.moysklad.intern.documentpatterns.repository.StockKey;
import ru.moysklad.intern.documentpatterns.state.CancelledState;
import ru.moysklad.intern.documentpatterns.state.DocumentContext;
import ru.moysklad.intern.documentpatterns.state.DraftState;
import ru.moysklad.intern.documentpatterns.state.PostedState;
import ru.moysklad.intern.documentpatterns.support.RecordingValidator;
import ru.moysklad.intern.documentpatterns.support.TestStocks;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidationChain;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidationException;
import ru.moysklad.intern.documentpatterns.validation.ValidationContext;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.operation.DocumentsType;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.MOMENT;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.acceptance;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.position;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.product;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.qty;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.shipment;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.transfer;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.warehouse;

/**
 * Проверяет готовую инфраструктуру лабораторной: модели, фикстуры, контексты, связывание звеньев, реестр.
 * Тесты включены и проходят до начала работы. Если они падают — сломана инфраструктура, а не решение.
 */
class DocumentPatternsInfrastructureTest {

    private final Product x = product("Товар X");
    private final Warehouse a = warehouse("Склад A");
    private final Warehouse b = warehouse("Склад B");

    @Test
    @DisplayName("Типы документов и требования к складам заданы подклассами")
    void documentTypesAndWarehouseRequirements() {
        AcceptanceDocument acceptance = new AcceptanceDocument(UUID.randomUUID(), MOMENT);
        ShipmentDocument shipment = new ShipmentDocument(UUID.randomUUID(), MOMENT);
        TransferDocument transfer = new TransferDocument(UUID.randomUUID(), MOMENT);

        assertThat(acceptance.getType()).isEqualTo(DocumentsType.GOODS_ACCEPTANCE);
        assertThat(shipment.getType()).isEqualTo(DocumentsType.SHIPMENT);
        assertThat(transfer.getType()).isEqualTo(DocumentsType.SHIFT);

        assertThat(acceptance.requiresSourceWarehouse()).isFalse();
        assertThat(acceptance.requiresTargetWarehouse()).isTrue();
        assertThat(shipment.requiresSourceWarehouse()).isTrue();
        assertThat(shipment.requiresTargetWarehouse()).isFalse();
        assertThat(transfer.requiresSourceWarehouse()).isTrue();
        assertThat(transfer.requiresTargetWarehouse()).isTrue();
    }

    @Test
    @DisplayName("Новый документ — черновик, позиции наружу только для чтения")
    void newDocumentIsEditableDraft() {
        ErpDocument document = acceptance(a, position(x, "10"));

        assertThat(document.getStatus()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(document.isEditable()).isTrue();
        assertThat(document.getPositions()).hasSize(1);
        assertThat(document.getPostedMovements()).isEmpty();
        assertThatThrownBy(() -> document.getPositions().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Не-черновик нельзя изменять: DOCUMENT_NOT_EDITABLE")
    void nonDraftIsNotEditable() {
        ErpDocument document = acceptance(a, position(x, "10"));
        document.applyStatus(DocumentStatus.POSTED);

        assertThatThrownBy(() -> document.addPosition(x, qty("1")))
                .isInstanceOfSatisfying(DocumentProcessingException.class,
                        e -> assertThat(e.getCode()).isEqualTo(DocumentErrorCode.DOCUMENT_NOT_EDITABLE));
        assertThatThrownBy(() -> document.setTargetWarehouse(b))
                .isInstanceOf(DocumentProcessingException.class);
        assertThat(document.getPositions()).hasSize(1);
    }

    @Test
    @DisplayName("Фикстуры: документы нужных типов со складами и позициями")
    void fixturesHaveExpectedShape() {
        ErpDocument shipment = shipment(a, position(x, "7"));
        ErpDocument transfer = transfer(a, b, position(x, "5"), position(product("Y"), "1"));

        assertThat(shipment.getSourceWarehouse()).isSameAs(a);
        assertThat(shipment.getTargetWarehouse()).isNull();
        assertThat(transfer.getPositions()).hasSize(2);
        assertThat(transfer.getPositions().getFirst().quantity()).isEqualByComparingTo("5");
        assertThat(TestStocks.withQuantity(x, a, "20").getQuantity(x, a)).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("Движение: знак дельты определяет приход/расход")
    void movementDirection() {
        StockMovement in = new StockMovement(UUID.randomUUID(), x, a, null, qty("1"));
        StockMovement out = new StockMovement(UUID.randomUUID(), x, a, null, qty("-1"));

        assertThat(in.isIncoming()).isTrue();
        assertThat(in.isOutgoing()).isFalse();
        assertThat(out.isOutgoing()).isTrue();
    }

    @Test
    @DisplayName("DocumentContext: статус меняется только в commit(), без плана commit() запрещён")
    void documentContextIsTwoPhase() {
        ErpDocument document = acceptance(a, position(x, "1"));
        DocumentContext context = new DocumentContext(document);

        assertThatThrownBy(context::commit).isInstanceOf(IllegalStateException.class);

        context.transitionTo(DocumentStatus.POSTED);
        assertThat(context.plannedStatus()).contains(DocumentStatus.POSTED);
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.DRAFT);
        assertThatThrownBy(() -> context.transitionTo(DocumentStatus.CANCELLED))
                .isInstanceOf(IllegalStateException.class);

        context.commit();
        assertThat(document.getStatus()).isEqualTo(DocumentStatus.POSTED);
        assertThat(context.plannedStatus()).isEmpty();
    }

    @Test
    @DisplayName("DocumentContext сопоставляет статусу объект-состояние")
    void documentContextResolvesState() {
        ErpDocument document = acceptance(a);
        assertThat(new DocumentContext(document).currentState()).isSameAs(DraftState.INSTANCE);
        document.applyStatus(DocumentStatus.POSTED);
        assertThat(new DocumentContext(document).currentState()).isSameAs(PostedState.INSTANCE);
        document.applyStatus(DocumentStatus.CANCELLED);
        assertThat(new DocumentContext(document).currentState()).isSameAs(CancelledState.INSTANCE);
        assertThat(CancelledState.INSTANCE.status()).isEqualTo(DocumentStatus.CANCELLED);
    }

    @Test
    @DisplayName("ValidationContext рассчитывает движения лениво и один раз")
    void validationContextMemoizesMovements() {
        AtomicInteger calls = new AtomicInteger();
        StockMovement movement = new StockMovement(UUID.randomUUID(), x, a, null, qty("1"));
        ValidationContext context = new ValidationContext(TestStocks.empty(), () -> {
            calls.incrementAndGet();
            return List.of(movement);
        });

        assertThat(context.movementsCalculated()).isFalse();
        assertThat(context.plannedMovements()).containsExactly(movement);
        assertThat(context.plannedMovements()).containsExactly(movement);
        assertThat(calls).hasValue(1);
        assertThat(context.availableQuantity(StockKey.of(x, a)))
                .isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Цепочка связывает звенья по порядку, ошибка звена прерывает её")
    void chainLinksValidatorsInOrder() {
        RecordingValidator first = new RecordingValidator();
        RecordingValidator failing = RecordingValidator.failingWith(DocumentErrorCode.EMPTY_DOCUMENT);
        RecordingValidator last = new RecordingValidator();
        DocumentValidationChain chain = DocumentValidationChain.of(first, failing, last);
        ValidationContext context = new ValidationContext(TestStocks.empty(), List::of);

        assertThat(chain.links()).containsExactly(first, failing, last);
        assertThatThrownBy(() -> chain.validate(acceptance(a), context))
                .isInstanceOfSatisfying(DocumentValidationException.class,
                        e -> assertThat(e.getCode()).isEqualTo(DocumentErrorCode.EMPTY_DOCUMENT));
        assertThat(first.invocations()).isEqualTo(1);
        assertThat(failing.invocations()).isEqualTo(1);
        assertThat(last.invocations()).isZero();
        assertThatThrownBy(() -> DocumentValidationChain.of(first, first)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Реестр находит фабрику по типу, дубли и неизвестные типы отклоняются")
    void factoryRegistry() {
        DocumentFactoryRegistry registry = new DocumentFactoryRegistry(List.of(
                new AcceptanceDocumentFactory(), new ShipmentDocumentFactory(), new TransferDocumentFactory()));

        assertThat(registry.supportedTypes())
                .containsExactlyInAnyOrder(DocumentsType.GOODS_ACCEPTANCE, DocumentsType.SHIPMENT, DocumentsType.SHIFT);
        assertThat(registry.forType(DocumentsType.SHIFT)).isInstanceOf(TransferDocumentFactory.class);
        assertThatThrownBy(() -> registry.forType(DocumentsType.MAKE))
                .isInstanceOfSatisfying(DocumentProcessingException.class,
                        e -> assertThat(e.getCode()).isEqualTo(DocumentErrorCode.UNSUPPORTED_DOCUMENT_TYPE));
        assertThatThrownBy(() -> new DocumentFactoryRegistry(
                List.of(new AcceptanceDocumentFactory(), new AcceptanceDocumentFactory())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("События содержат тип, документ, статус и код ошибки")
    void eventFactories() {
        ErpDocument document = shipment(a, position(x, "1"));

        DocumentEvent posted = DocumentEvent.of(DocumentEventType.DOCUMENT_POSTED, document);
        DocumentEvent failed = DocumentEvent.failed(document, DocumentErrorCode.INSUFFICIENT_STOCK);

        assertThat(posted.documentId()).isEqualTo(document.getId());
        assertThat(posted.documentType()).isEqualTo(DocumentsType.SHIPMENT);
        assertThat(posted.documentStatus()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(posted.errorCode()).isNull();
        assertThat(failed.type()).isEqualTo(DocumentEventType.DOCUMENT_PROCESSING_FAILED);
        assertThat(failed.errorCode()).isEqualTo(DocumentErrorCode.INSUFFICIENT_STOCK);
        assertThat(failed.eventId()).isNotEqualTo(posted.eventId());
    }

    @Test
    @DisplayName("Журнал аудита хранит записи и фильтрует по документу")
    void auditTrail() {
        AuditTrail trail = new AuditTrail();
        UUID documentId = UUID.randomUUID();
        trail.record(new AuditRecord(documentId, DocumentsType.SHIPMENT, "POST", DocumentStatus.DRAFT,
                DocumentStatus.POSTED, AuditOutcome.SUCCESS, null, Instant.now()));
        trail.record(new AuditRecord(UUID.randomUUID(), DocumentsType.SHIPMENT, "POST", DocumentStatus.DRAFT,
                DocumentStatus.DRAFT, AuditOutcome.FAILURE, "INSUFFICIENT_STOCK", Instant.now()));

        assertThat(trail.records()).hasSize(2);
        assertThat(trail.records(documentId)).singleElement()
                .extracting(AuditRecord::outcome).isEqualTo(AuditOutcome.SUCCESS);
    }
}
