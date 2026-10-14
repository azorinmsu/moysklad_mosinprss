package ru.moysklad.intern.dto.kafka;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Команда заявки на реализацию товара.
 * Формирование и публикация — задание студента ({@code SalesKafkaProducer}).
 */
public record SalesCommand(
        UUID requestId,
        UUID productId,
        UUID storeId,
        BigDecimal quantity,
        BigDecimal availableAmount,
        LocalDateTime requestedAt,
        UUID correlationId
) {
}
