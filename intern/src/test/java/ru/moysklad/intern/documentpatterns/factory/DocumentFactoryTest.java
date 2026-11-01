package ru.moysklad.intern.documentpatterns.factory;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Часть 1. Factory Method / Abstract Factory — создание документов и компонентов их обработки.
 */
class DocumentFactoryTest {

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Фабрика приёмки создаёт AcceptanceDocument типа GOODS_ACCEPTANCE")
    void createsAcceptance() {
        // TODO 1: new AcceptanceDocumentFactory().createDocument(id, MOMENT).
        // TODO 2: Проверить класс AcceptanceDocument и getType() == GOODS_ACCEPTANCE.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Фабрика отгрузки создаёт ShipmentDocument типа SHIPMENT")
    void createsShipment() {
        // TODO: аналогично для ShipmentDocumentFactory.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Фабрика перемещения создаёт TransferDocument типа SHIFT")
    void createsTransfer() {
        // TODO: аналогично для TransferDocumentFactory.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Новый документ — DRAFT, без позиций и проведённых движений")
    void newDocumentIsDraft() {
        // TODO: для каждой из трёх фабрик проверить статус DRAFT, пустые getPositions() и getPostedMovements().
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Фабрика устанавливает id и дату документа")
    void setsIdAndMoment() {
        // TODO: id и moment документа совпадают с переданными в createDocument.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Обязательные поля: null id или дата → IllegalArgumentException")
    void requiredArgumentsAreChecked() {
        // TODO: createDocument(null, MOMENT) и createDocument(id, null) → IllegalArgumentException.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Abstract Factory: фабрика создаёт согласованные стратегию и обработчик своего типа")
    void createsConsistentFamily() {
        // TODO 1: ProcessingInfrastructure из лямбды-валидатора (d, c) -> {}, TestStocks.empty(), нового publisher.
        // TODO 2: Для каждой фабрики проверить класс createMovementStrategy() и createProcessor(...).
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Ошибка реализации подкласса (не тот тип документа) обнаруживается в createDocument")
    void detectsBrokenFactorySubclass() {
        // TODO: анонимный подкласс AcceptanceDocumentFactory, у которого newDocument возвращает ShipmentDocument
        //  → createDocument бросает IllegalStateException.
        fail("TODO: implement test");
    }
}
