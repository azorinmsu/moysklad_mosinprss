package ru.moysklad.intern.documentpatterns.support;

import ru.moysklad.intern.documentpatterns.model.AcceptanceDocument;
import ru.moysklad.intern.documentpatterns.model.DocumentPosition;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.documentpatterns.model.ShipmentDocument;
import ru.moysklad.intern.documentpatterns.model.TransferDocument;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Тестовые данные: товары, склады и документы-черновики.
 *
 * <p>Документы создаются конструкторами модели напрямую — без студенческих фабрик и builder,
 * поэтому фикстуры работают до выполнения заданий. Бизнес-логики здесь нет.
 */
public final class TestDocuments {

    public static final LocalDateTime MOMENT = LocalDateTime.of(2026, 11, 1, 12, 0);

    private TestDocuments() {
    }

    public static Product product(String name) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName(name);
        return product;
    }

    public static Warehouse warehouse(String name) {
        Warehouse warehouse = new Warehouse(name);
        warehouse.setId(UUID.randomUUID());
        return warehouse;
    }

    public static Lot lot(String name) {
        Lot lot = new Lot(name);
        lot.setId(UUID.randomUUID());
        return lot;
    }

    public static BigDecimal qty(String value) {
        return new BigDecimal(value);
    }

    public static DocumentPosition position(Product product, String quantity) {
        return new DocumentPosition(product, qty(quantity));
    }

    public static DocumentPosition position(Product product, String quantity, Lot lot) {
        return new DocumentPosition(product, qty(quantity), lot);
    }

    /** Приёмка-черновик на склад {@code target}; {@code target} может быть {@code null} для негативных тестов. */
    public static AcceptanceDocument acceptance(Warehouse target, DocumentPosition... positions) {
        AcceptanceDocument document = new AcceptanceDocument(UUID.randomUUID(), MOMENT);
        document.setTargetWarehouse(target);
        return fill(document, positions);
    }

    /** Отгрузка-черновик со склада {@code source}. */
    public static ShipmentDocument shipment(Warehouse source, DocumentPosition... positions) {
        ShipmentDocument document = new ShipmentDocument(UUID.randomUUID(), MOMENT);
        document.setSourceWarehouse(source);
        return fill(document, positions);
    }

    /** Перемещение-черновик с {@code source} на {@code target}. */
    public static TransferDocument transfer(Warehouse source, Warehouse target, DocumentPosition... positions) {
        TransferDocument document = new TransferDocument(UUID.randomUUID(), MOMENT);
        document.setSourceWarehouse(source);
        document.setTargetWarehouse(target);
        return fill(document, positions);
    }

    private static <T extends ErpDocument> T fill(T document, DocumentPosition... positions) {
        for (DocumentPosition position : positions) {
            document.addPosition(position.product(), position.quantity(), position.lot());
        }
        return document;
    }
}
