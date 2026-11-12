package ru.moysklad.intern.documentpatterns.observer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.documentpatterns.command.DocumentCommandExecutor;
import ru.moysklad.intern.documentpatterns.command.PostDocumentCommand;
import ru.moysklad.intern.documentpatterns.command.UnpostDocumentCommand;
import ru.moysklad.intern.documentpatterns.factory.AcceptanceDocumentFactory;
import ru.moysklad.intern.documentpatterns.factory.ShipmentDocumentFactory;
import ru.moysklad.intern.documentpatterns.model.AcceptanceDocument;
import ru.moysklad.intern.documentpatterns.model.DocumentErrorCode;
import ru.moysklad.intern.documentpatterns.model.DocumentStatus;
import ru.moysklad.intern.documentpatterns.model.ShipmentDocument;
import ru.moysklad.intern.documentpatterns.processing.AcceptanceProcessor;
import ru.moysklad.intern.documentpatterns.processing.ProcessingInfrastructure;
import ru.moysklad.intern.documentpatterns.processing.ShipmentProcessor;
import ru.moysklad.intern.documentpatterns.repository.InMemoryStockRepository;
import ru.moysklad.intern.documentpatterns.support.RecordingEventListener;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidationChain;
import ru.moysklad.intern.documentpatterns.validation.DocumentValidationException;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.acceptance;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.position;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.product;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.shipment;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.warehouse;

/**
 * Часть 8. Observer — события документов.
 */
class DocumentObserverTest {

    @Test
    @DisplayName("Публикация доставляет событие подписчику")
    void publishDeliversEvent() {
        DocumentEventPublisher publisher = new DocumentEventPublisher();
        RecordingEventListener listener = new RecordingEventListener();
        publisher.subscribe(listener);

        DocumentEvent event = DocumentEvent.of(DocumentEventType.DOCUMENT_POSTED,
                acceptance(warehouse("A"), position(product("X"), "5")));
        publisher.publish(event);

        assertThat(listener.events()).containsExactly(event);
    }

    @Test
    @DisplayName("Несколько подписчиков в порядке подписки; повторная подписка не дублирует; отписка")
    void multipleSubscribers() {
        DocumentEventPublisher publisher = new DocumentEventPublisher();
        RecordingEventListener listener1 = new RecordingEventListener();
        RecordingEventListener listener2 = new RecordingEventListener();

        publisher.subscribe(listener1);
        publisher.subscribe(listener2);
        publisher.subscribe(listener1);

        assertThat(publisher.listeners()).containsExactly(listener1, listener2);

        DocumentEvent event1 = DocumentEvent.of(DocumentEventType.DOCUMENT_POSTED,
                acceptance(warehouse("A"), position(product("X"), "5")));
        publisher.publish(event1);

        assertThat(listener1.events()).containsExactly(event1);
        assertThat(listener2.events()).containsExactly(event1);

        publisher.unsubscribe(listener1);
        assertThat(publisher.listeners()).containsExactly(listener2);

        DocumentEvent event2 = DocumentEvent.of(DocumentEventType.DOCUMENT_UNPOSTED,
                acceptance(warehouse("A"), position(product("X"), "5")));
        publisher.publish(event2);

        assertThat(listener1.events()).containsExactly(event1);
        assertThat(listener2.events()).containsExactly(event1, event2);
    }

    @Test
    @DisplayName("Неуспешное проведение не публикует DOCUMENT_POSTED")
    void noPostedEventOnFailure() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        ShipmentDocument doc = shipment(a, position(x, "10"));

        DocumentEventPublisher publisher = new DocumentEventPublisher();
        RecordingEventListener listener = new RecordingEventListener();
        publisher.subscribe(listener);

        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                new InMemoryStockRepository(),
                publisher
        );
        ShipmentProcessor processor = (ShipmentProcessor) new ShipmentDocumentFactory().createProcessor(infra);

        assertThatThrownBy(() -> processor.process(doc))
                .isInstanceOf(DocumentValidationException.class);

        assertThat(listener.count(DocumentEventType.DOCUMENT_POSTED)).isZero();
        assertThat(listener.events()).hasSize(1);
        DocumentEvent failedEvent = listener.events().get(0);
        assertThat(failedEvent.type()).isEqualTo(DocumentEventType.DOCUMENT_PROCESSING_FAILED);
        assertThat(failedEvent.errorCode()).isEqualTo(DocumentErrorCode.INSUFFICIENT_STOCK);
    }

    @Test
    @DisplayName("Ошибка подписчика не ломает проведение и доставку остальным")
    void listenerFailureDoesNotBreakPosting() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        AcceptanceDocument doc = acceptance(a, position(x, "5"));

        DocumentEventPublisher publisher = new DocumentEventPublisher();
        DocumentEventListener failingListener = event -> {
            throw new RuntimeException("Listener error");
        };
        RecordingEventListener listener2 = new RecordingEventListener();

        publisher.subscribe(failingListener);
        publisher.subscribe(listener2);

        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                new InMemoryStockRepository(),
                publisher
        );
        AcceptanceProcessor processor = (AcceptanceProcessor) new AcceptanceDocumentFactory().createProcessor(infra);

        processor.process(doc);

        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.POSTED);
        assertThat(listener2.types()).contains(DocumentEventType.DOCUMENT_POSTED);
        assertThat(publisher.deliveryFailures()).hasSize(1);
        assertThat(publisher.deliveryFailures().get(0).error().getMessage()).isEqualTo("Listener error");
    }

    @Test
    @DisplayName("Повторная обработка той же операции не создаёт повторного уведомления")
    void noDuplicateNotifications() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        AcceptanceDocument doc = acceptance(a, position(x, "10"));

        DocumentEventPublisher publisher = new DocumentEventPublisher();
        List<String> notifications = new ArrayList<>();
        NotificationDocumentListener notificationListener = new NotificationDocumentListener(notifications::add);
        publisher.subscribe(notificationListener);

        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                new InMemoryStockRepository(),
                publisher
        );
        AcceptanceProcessor processor = (AcceptanceProcessor) new AcceptanceDocumentFactory().createProcessor(infra);

        DocumentCommandExecutor executor = new DocumentCommandExecutor();
        UUID operationId = UUID.randomUUID();
        PostDocumentCommand cmd1 = new PostDocumentCommand(operationId, doc, processor);
        PostDocumentCommand cmd2 = new PostDocumentCommand(operationId, doc, processor);

        executor.execute(cmd1);
        executor.execute(cmd2);

        assertThat(notifications).hasSize(1);

        AuditDocumentListener auditListener = new AuditDocumentListener();
        DocumentEvent event = DocumentEvent.of(DocumentEventType.DOCUMENT_POSTED, doc);

        auditListener.onEvent(event);
        auditListener.onEvent(event);
        assertThat(auditListener.journal()).hasSize(1);

        notificationListener.onEvent(event);
        notificationListener.onEvent(event);
        assertThat(notifications).hasSize(2);
    }

    @Test
    @DisplayName("Корректные типы событий и уведомления по ним")
    void eventTypesAndNotifications() {
        Warehouse a = warehouse("A");
        Product x = product("X");
        AcceptanceDocument doc = acceptance(a, position(x, "10"));

        DocumentEventPublisher publisher = new DocumentEventPublisher();
        RecordingEventListener recordingListener = new RecordingEventListener();
        List<String> notifications = new ArrayList<>();
        NotificationDocumentListener notificationListener = new NotificationDocumentListener(notifications::add);

        publisher.subscribe(recordingListener);
        publisher.subscribe(notificationListener);

        InMemoryStockRepository stocks = new InMemoryStockRepository();
        ProcessingInfrastructure infra = new ProcessingInfrastructure(
                DocumentValidationChain.defaultChain(),
                stocks,
                publisher
        );
        AcceptanceProcessor processor = (AcceptanceProcessor) new AcceptanceDocumentFactory().createProcessor(infra);

        processor.process(doc);
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.POSTED);
        assertThat(recordingListener.events().get(0).type()).isEqualTo(DocumentEventType.DOCUMENT_POSTED);
        assertThat(recordingListener.events().get(0).documentStatus()).isEqualTo(DocumentStatus.POSTED);

        DocumentCommandExecutor executor = new DocumentCommandExecutor();
        executor.execute(new UnpostDocumentCommand(UUID.randomUUID(), doc, stocks, publisher));
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.CANCELLED);
        assertThat(recordingListener.events().get(1).type()).isEqualTo(DocumentEventType.DOCUMENT_UNPOSTED);
        assertThat(recordingListener.events().get(1).documentStatus()).isEqualTo(DocumentStatus.CANCELLED);

        int beforeCreatedCount = notifications.size();
        DocumentEvent createdEvent = DocumentEvent.of(DocumentEventType.DOCUMENT_CREATED, doc);
        notificationListener.onEvent(createdEvent);
        assertThat(notifications).hasSize(beforeCreatedCount);

        DocumentEvent failedEvent = DocumentEvent.failed(doc, DocumentErrorCode.INSUFFICIENT_STOCK);
        notificationListener.onEvent(failedEvent);
        assertThat(notifications).hasSize(beforeCreatedCount + 1);
        assertThat(notifications.get(notifications.size() - 1)).contains(DocumentErrorCode.INSUFFICIENT_STOCK.name());
    }
}
