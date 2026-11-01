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
        // TODO: запомните id и верните this.
        throw new UnsupportedOperationException("TODO");
    }

    public DocumentBuilder moment(LocalDateTime moment) {
        // TODO: запомните дату документа и верните this. Если дату не задали, build() использует текущее время.
        throw new UnsupportedOperationException("TODO");
    }

    public DocumentBuilder sourceWarehouse(Warehouse warehouse) {
        // TODO: запомните склад-источник и верните this.
        throw new UnsupportedOperationException("TODO");
    }

    public DocumentBuilder targetWarehouse(Warehouse warehouse) {
        // TODO: запомните склад-получатель и верните this.
        throw new UnsupportedOperationException("TODO");
    }

    public DocumentBuilder addPosition(Product product, BigDecimal quantity) {
        // TODO: добавьте позицию без партии и верните this.
        throw new UnsupportedOperationException("TODO");
    }

    public DocumentBuilder addPosition(Product product, BigDecimal quantity, Lot lot) {
        // TODO:
        // 1. Создайте DocumentPosition (null-товар и null-количество отклоняет конструктор записи).
        // 2. Добавьте в positions и верните this.
        // Положительность количества здесь не проверяйте — это правило QuantityValidator.
        throw new UnsupportedOperationException("TODO");
    }

    public ErpDocument build() {
        // TODO:
        // 1. Обязательное поле id: если не задано — IllegalStateException с понятным сообщением.
        // 2. Дата: moment или LocalDateTime.now().
        // 3. Создайте документ через factory.createDocument(id, moment).
        // 4. Обязательные склады определяет документ: requiresSourceWarehouse()/requiresTargetWarehouse().
        //    Нужный склад не задан — IllegalStateException.
        // 5. Перенесите в документ заданные склады и все позиции.
        // 6. Верните документ. Повторный build() должен создавать новый независимый объект.
        throw new UnsupportedOperationException("TODO");
    }
}
