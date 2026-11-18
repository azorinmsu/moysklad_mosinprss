package ru.moysklad.concurrency.domain;

public record SalesOrder(
        String id,
        String customerId,
        String organizationId,
        String productId,
        String warehouseId,
        int quantity,
        OrderStatus status
) {
}
