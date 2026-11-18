package ru.moysklad.concurrency.domain;

public final class StockBalance {

    private final String productId;
    private final String warehouseId;
    private int quantity;
    private int reserved;

    public StockBalance(String productId, String warehouseId, int quantity) {
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getReserved() {
        return reserved;
    }

    public void setReserved(int reserved) {
        this.reserved = reserved;
    }

    public int available() {
        return quantity - reserved;
    }

    public StockBalance copy() {
        StockBalance copy = new StockBalance(productId, warehouseId, quantity);
        copy.reserved = reserved;
        return copy;
    }
}
