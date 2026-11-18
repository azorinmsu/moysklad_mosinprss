package ru.moysklad.concurrency.domain;

public record PurchaseOrder(
        String id,
        String supplierId,
        String productId,
        String warehouseId,
        int quantity,
        OrderStatus status
) {
}
