package ru.moysklad.concurrency.domain;

public class InsufficientStockException extends RuntimeException {

    private final String productId;
    private final String warehouseId;
    private final int requested;
    private final int available;

    public InsufficientStockException(String productId, String warehouseId, int requested, int available) {
        super("Недостаточно товара " + productId + " на складе " + warehouseId
                + ": запрошено " + requested + ", доступно " + available);
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.requested = requested;
        this.available = available;
    }

    public String getProductId() {
        return productId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
