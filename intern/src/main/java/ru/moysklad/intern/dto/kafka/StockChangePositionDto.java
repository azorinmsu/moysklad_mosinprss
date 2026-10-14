package ru.moysklad.intern.dto.kafka;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StockChangePositionDto(
        UUID productId,
        UUID storeId,
        UUID destinationStoreId,
        UUID consignmentId,
        BigDecimal quantity,
        List<String> serialNumbers,
        BigDecimal unitPrice
) {
}
