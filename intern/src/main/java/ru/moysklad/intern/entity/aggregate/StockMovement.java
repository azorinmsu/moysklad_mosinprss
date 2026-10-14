package ru.moysklad.intern.entity.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_movement")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "document_position_id")
    private UUID documentPositionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Warehouse store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consignment_id", nullable = false)
    private Lot consignment;

    @Column(name = "quantity_delta", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantityDelta;

    @Column(name = "moment", nullable = false)
    private LocalDateTime moment;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 64)
    private StockMovementOperationType operationType;

    public StockMovement() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public UUID getDocumentPositionId() {
        return documentPositionId;
    }

    public void setDocumentPositionId(UUID documentPositionId) {
        this.documentPositionId = documentPositionId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Warehouse getStore() {
        return store;
    }

    public void setStore(Warehouse store) {
        this.store = store;
    }

    public Lot getConsignment() {
        return consignment;
    }

    public void setConsignment(Lot consignment) {
        this.consignment = consignment;
    }

    public BigDecimal getQuantityDelta() {
        return quantityDelta;
    }

    public void setQuantityDelta(BigDecimal quantityDelta) {
        this.quantityDelta = quantityDelta;
    }

    public LocalDateTime getMoment() {
        return moment;
    }

    public void setMoment(LocalDateTime moment) {
        this.moment = moment;
    }

    public StockMovementOperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(StockMovementOperationType operationType) {
        this.operationType = operationType;
    }
}
