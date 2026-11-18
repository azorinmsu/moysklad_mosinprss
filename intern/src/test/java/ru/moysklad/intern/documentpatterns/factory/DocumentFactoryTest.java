package ru.moysklad.intern.documentpatterns.factory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.documentpatterns.model.AcceptanceDocument;
import ru.moysklad.intern.documentpatterns.model.DocumentStatus;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.ShipmentDocument;
import ru.moysklad.intern.documentpatterns.model.TransferDocument;
import ru.moysklad.intern.documentpatterns.observer.DocumentEventPublisher;
import ru.moysklad.intern.documentpatterns.processing.AcceptanceProcessor;
import ru.moysklad.intern.documentpatterns.processing.ProcessingInfrastructure;
import ru.moysklad.intern.documentpatterns.processing.ShipmentProcessor;
import ru.moysklad.intern.documentpatterns.processing.TransferProcessor;
import ru.moysklad.intern.documentpatterns.strategy.AcceptanceMovementStrategy;
import ru.moysklad.intern.documentpatterns.strategy.ShipmentMovementStrategy;
import ru.moysklad.intern.documentpatterns.strategy.TransferMovementStrategy;
import ru.moysklad.intern.documentpatterns.support.TestStocks;
import ru.moysklad.intern.entity.operation.DocumentsType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.MOMENT;

/**
 * Часть 1. Factory Method / Abstract Factory — создание документов и компонентов их обработки.
 */
class DocumentFactoryTest {

    @Test
    @DisplayName("Фабрика приёмки создаёт AcceptanceDocument типа GOODS_ACCEPTANCE")
    void createsAcceptance() {
        UUID id = UUID.randomUUID();
        ErpDocument doc = new AcceptanceDocumentFactory().createDocument(id, MOMENT);
        assertThat(doc).isInstanceOf(AcceptanceDocument.class);
        assertThat(doc.getType()).isEqualTo(DocumentsType.GOODS_ACCEPTANCE);
    }

    @Test
    @DisplayName("Фабрика отгрузки создаёт ShipmentDocument типа SHIPMENT")
    void createsShipment() {
        UUID id = UUID.randomUUID();
        ErpDocument doc = new ShipmentDocumentFactory().createDocument(id, MOMENT);
        assertThat(doc).isInstanceOf(ShipmentDocument.class);
        assertThat(doc.getType()).isEqualTo(DocumentsType.SHIPMENT);
    }

    @Test
    @DisplayName("Фабрика перемещения создаёт TransferDocument типа SHIFT")
    void createsTransfer() {
        UUID id = UUID.randomUUID();
        ErpDocument doc = new TransferDocumentFactory().createDocument(id, MOMENT);
        assertThat(doc).isInstanceOf(TransferDocument.class);
        assertThat(doc.getType()).isEqualTo(DocumentsType.SHIFT);
    }

    @Test
    @DisplayName("Новый документ — DRAFT, без позиций и проведённых движений")
    void newDocumentIsDraft() {
        UUID id = UUID.randomUUID();
        for (DocumentFactory factory : List.of(new AcceptanceDocumentFactory(), new ShipmentDocumentFactory(), new TransferDocumentFactory())) {
            ErpDocument doc = factory.createDocument(id, MOMENT);
            assertThat(doc.getStatus()).isEqualTo(DocumentStatus.DRAFT);
            assertThat(doc.getPositions()).isEmpty();
            assertThat(doc.getPostedMovements()).isEmpty();
        }
    }

    @Test
    @DisplayName("Фабрика устанавливает id и дату документа")
    void setsIdAndMoment() {
        UUID id = UUID.randomUUID();
        ErpDocument doc = new AcceptanceDocumentFactory().createDocument(id, MOMENT);
        assertThat(doc.getId()).isEqualTo(id);
        assertThat(doc.getMoment()).isEqualTo(MOMENT);
    }

    @Test
    @DisplayName("Обязательные поля: null id или дата → IllegalArgumentException")
    void requiredArgumentsAreChecked() {
        UUID id = UUID.randomUUID();
        AcceptanceDocumentFactory factory = new AcceptanceDocumentFactory();
        assertThatThrownBy(() -> factory.createDocument(null, MOMENT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factory.createDocument(id, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Abstract Factory: фабрика создаёт согласованные стратегию и обработчик своего типа")
    void createsConsistentFamily() {
        ProcessingInfrastructure infrastructure = new ProcessingInfrastructure(
                (d, c) -> {},
                TestStocks.empty(),
                new DocumentEventPublisher()
        );

        AcceptanceDocumentFactory acceptanceFactory = new AcceptanceDocumentFactory();
        assertThat(acceptanceFactory.createMovementStrategy()).isInstanceOf(AcceptanceMovementStrategy.class);
        assertThat(acceptanceFactory.createProcessor(infrastructure)).isInstanceOf(AcceptanceProcessor.class);

        ShipmentDocumentFactory shipmentFactory = new ShipmentDocumentFactory();
        assertThat(shipmentFactory.createMovementStrategy()).isInstanceOf(ShipmentMovementStrategy.class);
        assertThat(shipmentFactory.createProcessor(infrastructure)).isInstanceOf(ShipmentProcessor.class);

        TransferDocumentFactory transferFactory = new TransferDocumentFactory();
        assertThat(transferFactory.createMovementStrategy()).isInstanceOf(TransferMovementStrategy.class);
        assertThat(transferFactory.createProcessor(infrastructure)).isInstanceOf(TransferProcessor.class);
    }

    @Test
    @DisplayName("Ошибка реализации подкласса (не тот тип документа) обнаруживается в createDocument")
    void detectsBrokenFactorySubclass() {
        DocumentFactory brokenFactory = new AcceptanceDocumentFactory() {
            @Override
            protected ErpDocument newDocument(UUID id, LocalDateTime moment) {
                return new ShipmentDocument(id, moment);
            }
        };
        assertThatThrownBy(() -> brokenFactory.createDocument(UUID.randomUUID(), MOMENT))
                .isInstanceOf(IllegalStateException.class);
    }
}
