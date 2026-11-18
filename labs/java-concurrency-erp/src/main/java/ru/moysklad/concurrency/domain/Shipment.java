package ru.moysklad.concurrency.domain;

public record Shipment(
        String id,
        String orderId,
        String productId,
        String warehouseId,
        int quantity
) {
}
