package ru.moysklad.intern.entity.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "aggregate_stock",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_aggregate_stock_product_store_consignment",
                columnNames = {"product_id", "store_id", "consignment_id"}
        )
)
public class AggregateStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Warehouse store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consignment_id", nullable = false)
    private Lot consignment;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public AggregateStock() {
    }

    public AggregateStock(Product product, Warehouse store, Lot consignment, BigDecimal quantity) {
        this.product = product;
        this.store = store;
        this.consignment = consignment;
        this.quantity = quantity;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
