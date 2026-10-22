package ru.moysklad.intern.remapstub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "remap_stub_stock")
public class StubStock {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal reserve;

    @Column(name = "in_transit", nullable = false, precision = 19, scale = 6)
    private BigDecimal inTransit;

    @Version
    private long version;

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public void setStoreId(UUID storeId) {
        this.storeId = storeId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getReserve() {
        return reserve;
    }

    public void setReserve(BigDecimal reserve) {
        this.reserve = reserve;
    }

    public BigDecimal getInTransit() {
        return inTransit;
    }

    public void setInTransit(BigDecimal inTransit) {
        this.inTransit = inTransit;
    }

    public BigDecimal available() {
        return quantity.subtract(reserve).subtract(inTransit);
    }

    public long getVersion() {
        return version;
    }
}
