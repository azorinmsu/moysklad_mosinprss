package ru.moysklad.intern.documentpatterns.model;

import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.operation.DocumentsType;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Складской документ учебной ERP-модели.
 *
 * <p>Тип документа — существующий {@link DocumentsType}; склад — {@link Warehouse}; товар — {@link Product}.
 * JPA-сущность {@code DocumentsWithPositions} здесь не используется: её позиции не содержат товара и количества
 * (адаптер к ней — задание повышенной сложности).
 *
 * <p>Инварианты, которые обеспечивает сам класс:
 * <ul>
 *     <li>реквизиты и позиции можно менять только в статусе {@link DocumentStatus#DRAFT};</li>
 *     <li>список позиций наружу отдаётся только для чтения;</li>
 *     <li>статус меняется только через {@link #applyStatus(DocumentStatus)}, который вызывает
 *     {@code DocumentContext.commit()} пакета {@code state}.</li>
 * </ul>
 *
 * <p>Конкретный подкласс определяет тип и требования к складам — так новые типы документов
 * добавляются без правки валидаторов ({@link #requiresSourceWarehouse()}, {@link #requiresTargetWarehouse()}).
 */
public abstract class ErpDocument {

    private final UUID id;
    private final LocalDateTime moment;
    private DocumentStatus status = DocumentStatus.DRAFT;
    private Warehouse sourceWarehouse;
    private Warehouse targetWarehouse;
    private final List<DocumentPosition> positions = new ArrayList<>();
    private List<StockMovement> postedMovements = List.of();

    protected ErpDocument(UUID id, LocalDateTime moment) {
        this.id = id;
        this.moment = moment;
    }

    public abstract DocumentsType getType();

    /** Нужен ли документу склад-источник (откуда списываем). */
    public abstract boolean requiresSourceWarehouse();

    /** Нужен ли документу склад-получатель (куда приходуем). */
    public abstract boolean requiresTargetWarehouse();

    public UUID getId() {
        return id;
    }

    public LocalDateTime getMoment() {
        return moment;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public Warehouse getSourceWarehouse() {
        return sourceWarehouse;
    }

    public Warehouse getTargetWarehouse() {
        return targetWarehouse;
    }

    public void setSourceWarehouse(Warehouse sourceWarehouse) {
        ensureEditable();
        this.sourceWarehouse = sourceWarehouse;
    }

    public void setTargetWarehouse(Warehouse targetWarehouse) {
        ensureEditable();
        this.targetWarehouse = targetWarehouse;
    }

    public DocumentPosition addPosition(Product product, BigDecimal quantity) {
        return addPosition(product, quantity, null);
    }

    public DocumentPosition addPosition(Product product, BigDecimal quantity, Lot lot) {
        ensureEditable();
        DocumentPosition position = new DocumentPosition(product, quantity, lot);
        positions.add(position);
        return position;
    }

    /** Снимок позиций, изменения снимка не влияют на документ. */
    public List<DocumentPosition> getPositions() {
        return List.copyOf(positions);
    }

    public boolean isEditable() {
        return status == DocumentStatus.DRAFT;
    }

    /**
     * Низкоуровневая смена статуса. Правила допустимых переходов здесь <b>не</b> проверяются —
     * их знает только паттерн State. Вызывайте метод только из {@code DocumentContext.commit()}.
     */
    public void applyStatus(DocumentStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("Статус не задан");
        }
        this.status = newStatus;
    }

    /** Движения, фактически применённые при проведении (нужны для отмены проведения). */
    public List<StockMovement> getPostedMovements() {
        return postedMovements;
    }

    public void recordPostedMovements(List<StockMovement> movements) {
        this.postedMovements = movements == null ? List.of() : List.copyOf(movements);
    }

    private void ensureEditable() {
        if (!isEditable()) {
            throw new DocumentProcessingException(DocumentErrorCode.DOCUMENT_NOT_EDITABLE,
                    "Документ " + id + " в статусе " + status + " нельзя изменять");
        }
    }

    @Override
    public String toString() {
        return getType() + "[" + id + ", " + status + ", позиций: " + positions.size() + "]";
    }
}
