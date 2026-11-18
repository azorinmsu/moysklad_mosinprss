package ru.moysklad.concurrency.domain;

public record Reservation(
        String id,
        String orderId,
        String productId,
        String warehouseId,
        int quantity
) {
}
