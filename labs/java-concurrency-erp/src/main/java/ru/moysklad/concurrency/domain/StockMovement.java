package ru.moysklad.concurrency.domain;

public record StockMovement(
        String id,
        String productId,
        String warehouseId,
        int quantity,
        String reason
) {
}
