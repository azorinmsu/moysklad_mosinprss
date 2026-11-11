package ru.moysklad.intern.documentpatterns.builder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.moysklad.intern.documentpatterns.model.AcceptanceDocument;
import ru.moysklad.intern.documentpatterns.model.DocumentStatus;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.ShipmentDocument;
import ru.moysklad.intern.documentpatterns.model.TransferDocument;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.MOMENT;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.lot;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.product;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.qty;
import static ru.moysklad.intern.documentpatterns.support.TestDocuments.warehouse;

/**
 * Часть 9. Builder — создание документов с позициями.
 */
class DocumentBuilderTest {

    @Test
    @DisplayName("Создание приёмки: тип, склад-получатель, позиция, статус DRAFT")
    void buildsAcceptance() {
        UUID id = UUID.randomUUID();
        Warehouse a = warehouse("A");
        Product x = product("X");

        ErpDocument doc = DocumentBuilder.acceptance()
                .id(id)
                .targetWarehouse(a)
                .addPosition(x, qty("10"))
                .build();

        assertThat(doc).isInstanceOf(AcceptanceDocument.class);
        assertThat(doc.getId()).isEqualTo(id);
        assertThat(doc.getTargetWarehouse()).isEqualTo(a);
        assertThat(doc.getSourceWarehouse()).isNull();
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(doc.getPositions()).hasSize(1);
        assertThat(doc.getPositions().get(0).product()).isEqualTo(x);
        assertThat(doc.getPositions().get(0).quantity()).isEqualByComparingTo(qty("10"));
    }

    @Test
    @DisplayName("Создание отгрузки")
    void buildsShipment() {
        UUID id = UUID.randomUUID();
        Warehouse a = warehouse("A");
        Product x = product("X");

        ErpDocument doc = DocumentBuilder.shipment()
                .id(id)
                .sourceWarehouse(a)
                .addPosition(x, qty("5"))
                .build();

        assertThat(doc).isInstanceOf(ShipmentDocument.class);
        assertThat(doc.getId()).isEqualTo(id);
        assertThat(doc.getSourceWarehouse()).isEqualTo(a);
        assertThat(doc.getTargetWarehouse()).isNull();
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.DRAFT);
        assertThat(doc.getPositions()).hasSize(1);
    }

    @Test
    @DisplayName("Создание перемещения с датой")
    void buildsTransfer() {
        UUID id = UUID.randomUUID();
        Warehouse a = warehouse("A");
        Warehouse b = warehouse("B");

        ErpDocument doc = DocumentBuilder.transfer()
                .id(id)
                .sourceWarehouse(a)
                .targetWarehouse(b)
                .moment(MOMENT)
                .build();

        assertThat(doc).isInstanceOf(TransferDocument.class);
        assertThat(doc.getId()).isEqualTo(id);
        assertThat(doc.getSourceWarehouse()).isEqualTo(a);
        assertThat(doc.getTargetWarehouse()).isEqualTo(b);
        assertThat(doc.getMoment()).isEqualTo(MOMENT);
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.DRAFT);
    }

    @Test
    @DisplayName("Несколько позиций сохраняют порядок и партию")
    void multiplePositions() {
        Product x = product("X");
        Product y = product("Y");
        Lot l = lot("L1");

        ErpDocument doc = DocumentBuilder.acceptance()
                .id(UUID.randomUUID())
                .targetWarehouse(warehouse("A"))
                .addPosition(x, qty("10"))
                .addPosition(y, qty("5"), l)
                .build();

        assertThat(doc.getPositions()).hasSize(2);
        assertThat(doc.getPositions().get(0).product()).isEqualTo(x);
        assertThat(doc.getPositions().get(0).quantity()).isEqualByComparingTo(qty("10"));
        assertThat(doc.getPositions().get(0).lot()).isNull();

        assertThat(doc.getPositions().get(1).product()).isEqualTo(y);
        assertThat(doc.getPositions().get(1).quantity()).isEqualByComparingTo(qty("5"));
        assertThat(doc.getPositions().get(1).lot()).isEqualTo(l);
    }

    @Test
    @DisplayName("Отсутствие обязательного поля → IllegalStateException")
    void missingRequiredField() {
        // без id
        assertThatThrownBy(() -> DocumentBuilder.acceptance()
                .targetWarehouse(warehouse("A"))
                .build())
                .isInstanceOf(IllegalStateException.class);

        // приёмка без склада-получателя
        assertThatThrownBy(() -> DocumentBuilder.acceptance()
                .id(UUID.randomUUID())
                .build())
                .isInstanceOf(IllegalStateException.class);

        // перемещение без склада-источника
        assertThatThrownBy(() -> DocumentBuilder.transfer()
                .id(UUID.randomUUID())
                .targetWarehouse(warehouse("B"))
                .build())
                .isInstanceOf(IllegalStateException.class);

        // перемещение без склада-получателя
        assertThatThrownBy(() -> DocumentBuilder.transfer()
                .id(UUID.randomUUID())
                .sourceWarehouse(warehouse("A"))
                .build())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Созданные объекты независимы от builder и друг от друга")
    void builtDocumentsAreIndependent() {
        DocumentBuilder builder = DocumentBuilder.acceptance()
                .id(UUID.randomUUID())
                .targetWarehouse(warehouse("A"))
                .addPosition(product("X"), qty("1"));

        ErpDocument doc1 = builder.build();

        builder.id(UUID.randomUUID());
        builder.addPosition(product("Y"), qty("2"));
        ErpDocument doc2 = builder.build();

        assertThat(doc1).isNotSameAs(doc2);
        assertThat(doc1.getPositions()).hasSize(1);
        assertThat(doc2.getPositions()).hasSize(2);

        doc1.addPosition(product("Z"), qty("3"));
        assertThat(doc1.getPositions()).hasSize(2);
        assertThat(doc2.getPositions()).hasSize(2);
        assertThat(doc2.getPositions().get(1).product().getName()).isEqualTo("Y");
    }

    @Test
    @DisplayName("Без явной даты используется текущее время")
    void defaultMoment() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        ErpDocument doc = DocumentBuilder.acceptance()
                .id(UUID.randomUUID())
                .targetWarehouse(warehouse("A"))
                .build();

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(doc.getMoment()).isAfterOrEqualTo(before).isBeforeOrEqualTo(after);
    }
}
