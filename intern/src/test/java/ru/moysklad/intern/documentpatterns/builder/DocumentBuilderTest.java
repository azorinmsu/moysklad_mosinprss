package ru.moysklad.intern.documentpatterns.builder;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Часть 9. Builder — создание документов с позициями.
 */
class DocumentBuilderTest {

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Создание приёмки: тип, склад-получатель, позиция, статус DRAFT")
    void buildsAcceptance() {
        // TODO: DocumentBuilder.acceptance().id(id).targetWarehouse(a).addPosition(x, 10).build().
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Создание отгрузки")
    void buildsShipment() {
        // TODO: shipment().sourceWarehouse(a)... → ShipmentDocument, источник a.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Создание перемещения с датой")
    void buildsTransfer() {
        // TODO: transfer().source(a).target(b).moment(MOMENT) → оба склада и дата.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Несколько позиций сохраняют порядок и партию")
    void multiplePositions() {
        // TODO: addPosition(x, 10), addPosition(y, 5, lot) → две позиции по порядку.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Отсутствие обязательного поля → IllegalStateException")
    void missingRequiredField() {
        // TODO: без id; приёмка без склада-получателя; перемещение без склада-источника.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Созданные объекты независимы от builder и друг от друга")
    void builtDocumentsAreIndependent() {
        // TODO: build() дважды → разные экземпляры; добавление позиции в builder после build
        //  и в первый документ не влияет на второй.
        fail("TODO: implement test");
    }

    @Test
    @Disabled("TODO: implement test")
    @DisplayName("Без явной даты используется текущее время")
    void defaultMoment() {
        // TODO: moment() не вызван → дата между «до» и «после» build().
        fail("TODO: implement test");
    }
}
