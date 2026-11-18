package ru.moysklad.intern.documentpatterns.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.documentpatterns.processing.ProcessingInfrastructure;
import ru.moysklad.intern.documentpatterns.factory.ShipmentDocumentFactory;
import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentStatus;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.ShipmentDocument;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventPublisher;
import ru.moysklad.intern.documentpatterns.processing.ShipmentProcessor;
import ru.moysklad.intern.documentpatterns.repository.InMemoryStockRepository;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidationChain;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidationException;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.acceptance;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.position;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.product;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.shipment;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.warehouse;

/**
 * Часть 7. State — жизненный цикл документа.
 */
class DocumentStateTest {

    @Test
    @DisplayName("DRAFT → POSTED: переход запланирован, применяется только в commit()")
    void draftToPosted() {
        ErpDocument doc = acceptance(warehouse("A"), position(product("X"), "10"));
        DocumentContext context = new DocumentContext(doc);
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(context.plannedStatus()).isEmpty();

        context.post();
        assertThat(context.plannedStatus()).contains(DocumentStatus.POSTED);
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.DRAFT);

        context.commit();
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.POSTED);
        assertThat(context.plannedStatus()).isEmpty();
    }

    @Test
    @DisplayName("POSTED → CANCELLED")
    void postedToCancelled() {
        ErpDocument doc = acceptance(warehouse("A"), position(product("X"), "10"));
        DocumentContext postContext = new DocumentContext(doc);
        postContext.post();
        postContext.commit();
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.POSTED);

        DocumentContext unpostContext = new DocumentContext(doc);
        unpostContext.unpost();
        assertThat(unpostContext.plannedStatus()).contains(DocumentStatus.CANCELLED);
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.POSTED);

        unpostContext.commit();
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.CANCELLED);
        assertThat(unpostContext.plannedStatus()).isEmpty();
    }

    @Test
    @DisplayName("Повторное проведение → ALREADY_POSTED, переход не запланирован")
    void repeatedPostIsForbidden() {
        ErpDocument doc = acceptance(warehouse("A"), position(product("X"), "10"));
        DocumentContext postContext = new DocumentContext(doc);
        postContext.post();
        postContext.commit();

        DocumentContext repeatedContext = new DocumentContext(doc);
        assertThatThrownBy(repeatedContext::post)
                .isInstanceOf(DocumentStateException.class)
                .satisfies(ex -> assertThat(((DocumentStateException) ex).getCode()).isEqualTo(DocumentErrorCode.ALREADY_POSTED));
        assertThat(repeatedContext.plannedStatus()).isEmpty();
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.POSTED);
    }

    @Test
    @DisplayName("Отмена черновика → NOT_POSTED")
    void unpostDraftIsForbidden() {
        ErpDocument doc = acceptance(warehouse("A"), position(product("X"), "10"));
        DocumentContext context = new DocumentContext(doc);

        assertThatThrownBy(context::unpost)
                .isInstanceOf(DocumentStateException.class)
                .satisfies(ex -> assertThat(((DocumentStateException) ex).getCode()).isEqualTo(DocumentErrorCode.NOT_POSTED));
        assertThat(context.plannedStatus()).isEmpty();
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.DRAFT);
    }

    @Test
    @DisplayName("CANCELLED — конечное состояние: повторная отмена и проведение → ALREADY_CANCELLED")
    void cancelledIsTerminal() {
        ErpDocument doc = acceptance(warehouse("A"), position(product("X"), "10"));
        DocumentContext postContext = new DocumentContext(doc);
        postContext.post();
        postContext.commit();
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.POSTED);

        DocumentContext unpostContext = new DocumentContext(doc);
        unpostContext.unpost();
        unpostContext.commit();
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.CANCELLED);

        DocumentContext cancelledContext1 = new DocumentContext(doc);
        assertThatThrownBy(cancelledContext1::post)
                .isInstanceOf(DocumentStateException.class)
                .satisfies(ex -> assertThat(((DocumentStateException) ex).getCode()).isEqualTo(DocumentErrorCode.ALREADY_CANCELLED));
        assertThat(cancelledContext1.plannedStatus()).isEmpty();

        DocumentContext cancelledContext2 = new DocumentContext(doc);
        assertThatThrownBy(cancelledContext2::unpost)
                .isInstanceOf(DocumentStateException.class)
                .satisfies(ex -> assertThat(((DocumentStateException) ex).getCode()).isEqualTo(DocumentErrorCode.ALREADY_CANCELLED));
        assertThat(cancelledContext2.plannedStatus()).isEmpty();
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.CANCELLED);
    }

    @Test
    @DisplayName("Ошибка выполнения операции сохраняет предыдущий статус")
    void failureKeepsPreviousStatus() {
        // 1: Обработчик отгрузки при нехватке → документ остаётся DRAFT.
        Warehouse a = warehouse("A");
        Product x = product("X");
        ShipmentDocument doc = shipment(a, position(x, "10"));
        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                new InMemoryStockRepository(),
                new DocumentEventPublisher()
        );
        ShipmentProcessor processor = (ShipmentProcessor) new ShipmentDocumentFactory().createProcessor(infra);

        assertThatThrownBy(() -> processor.process(doc))
                .isInstanceOf(DocumentValidationException.class)
                .satisfies(ex -> assertThat(((DocumentValidationException) ex).getCode()).isEqualTo(DocumentErrorCode.INSUFFICIENT_STOCK));
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.DRAFT);

        // 2: context.post() без commit() (операция «упала») → статус DRAFT.
        ErpDocument draftDoc = acceptance(a, position(x, "5"));
        DocumentContext context = new DocumentContext(draftDoc);
        context.post();
        // симулируем падение до commit()
        assertThat(draftDoc.getStatus()).isEqualTo(DocumentStatus.DRAFT);
    }

    @Test
    @DisplayName("Каждое состояние соответствует своему статусу")
    void statesMatchStatuses() {
        assertThat(DraftState.INSTANCE.status()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(PostedState.INSTANCE.status()).isEqualTo(DocumentStatus.POSTED);
        assertThat(CancelledState.INSTANCE.status()).isEqualTo(DocumentStatus.CANCELLED);

        ErpDocument draftDoc = acceptance(warehouse("A"), position(product("X"), "1"));
        assertThat(new DocumentContext(draftDoc).currentState()).isSameAs(DraftState.INSTANCE);

        draftDoc.applyStatus(DocumentStatus.POSTED);
        assertThat(new DocumentContext(draftDoc).currentState()).isSameAs(PostedState.INSTANCE);

        draftDoc.applyStatus(DocumentStatus.CANCELLED);
        assertThat(new DocumentContext(draftDoc).currentState()).isSameAs(CancelledState.INSTANCE);
    }
}
