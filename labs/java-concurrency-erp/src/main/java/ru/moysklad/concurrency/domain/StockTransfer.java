package ru.moysklad.concurrency.domain;

public record StockTransfer(
        String id,
        String productId,
        String fromWarehouseId,
        String toWarehouseId,
        int quantity
) {
}
