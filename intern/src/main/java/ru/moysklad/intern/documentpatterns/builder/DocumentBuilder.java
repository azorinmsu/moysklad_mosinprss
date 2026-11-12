package ru.moysklad.intern.documentpatterns.builder;

import ru.moysklad.intern.documentpatterns.factory.AcceptanceDocumentFactory;
import ru.moysklad.intern.documentpatterns.factory.DocumentFactory;
import ru.moysklad.intern.documentpatterns.factory.ShipmentDocumentFactory;
import ru.moysklad.intern.documentpatterns.factory.TransferDocumentFactory;
import ru.moysklad.intern.documentpatterns.model.DocumentPosition;
import ru.moysklad.intern.documentpatterns.model.ErpDocument;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Паттерн Builder: пошаговое создание документа с реквизитами и позициями.
 *
 * <pre>{@code
 * ErpDocument document = DocumentBuilder.acceptance()
 *         .id(UUID.randomUUID())
 *         .targetWarehouse(warehouse)
 *         .addPosition(productA, new BigDecimal("10"))
 *         .addPosition(productB, new BigDecimal("5"))
 *         .build();
 * }</pre>
 *
 * <p>Сам экземпляр документа создаёт фабрика ({@link DocumentFactory#createDocument}), builder лишь
 * накапливает параметры и проверяет обязательные поля. Builder можно переиспользовать:
 * каждый {@link #build()} возвращает новый независимый документ.
 */
public final class DocumentBuilder {

    private final DocumentFactory factory;
    private UUID id;
    private LocalDateTime moment;
    private Warehouse sourceWarehouse;
    private Warehouse targetWarehouse;
    private final List<DocumentPosition> positions = new ArrayList<>();

    private DocumentBuilder(DocumentFactory factory) {
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    public static DocumentBuilder acceptance() {
        return forFactory(new AcceptanceDocumentFactory());
    }

    public static DocumentBuilder shipment() {
        return forFactory(new ShipmentDocumentFactory());
    }

    public static DocumentBuilder transfer() {
        return forFactory(new TransferDocumentFactory());
    }

    public static DocumentBuilder forFactory(DocumentFactory factory) {
        return new DocumentBuilder(factory);
    }

    public DocumentBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public DocumentBuilder moment(LocalDateTime moment) {
        this.moment = moment;
        return this;
    }

    public DocumentBuilder sourceWarehouse(Warehouse warehouse) {
        this.sourceWarehouse = warehouse;
        return this;
    }

    public DocumentBuilder targetWarehouse(Warehouse warehouse) {
        this.targetWarehouse = warehouse;
        return this;
    }

    public DocumentBuilder addPosition(Product product, BigDecimal quantity) {
        positions.add(new DocumentPosition(product, quantity));
        return this;
    }

    public DocumentBuilder addPosition(Product product, BigDecimal quantity, Lot lot) {
        positions.add(new DocumentPosition(product, quantity, lot));
        return this;
    }

    public ErpDocument build() {
        if (id == null)
            throw new IllegalStateException("ID документа не может быть null");
        LocalDateTime docMoment = moment == null ? LocalDateTime.now() : moment;

        ErpDocument doc = factory.createDocument(id, docMoment);

        if (doc.requiresSourceWarehouse() && sourceWarehouse == null)
            throw new IllegalStateException("Для документа типа " + doc.getType() + " требуется склад-источник");

        if (doc.requiresTargetWarehouse() && targetWarehouse == null)
            throw new IllegalStateException("Для документа типа " + doc.getType() + " требуется склад-получатель");

        if (sourceWarehouse != null)
            doc.setSourceWarehouse(sourceWarehouse);
        if (targetWarehouse != null)
            doc.setTargetWarehouse(targetWarehouse);

        for (DocumentPosition pos : positions)
            doc.addPosition(pos.product(), pos.quantity(), pos.lot());

        return doc;
    }
}
