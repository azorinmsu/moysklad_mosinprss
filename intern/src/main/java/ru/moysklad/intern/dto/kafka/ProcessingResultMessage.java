package ru.moysklad.intern.dto.kafka;

import ru.moysklad.intern.entity.processing.ProcessingResultStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сообщение результата обработки в топике {@code processing.results}.
 * Не путать с JPA-сущностью {@link ru.moysklad.intern.entity.processing.ProcessingResult}.
 */
public record ProcessingResultMessage(
        UUID requestId,
        ProcessingResultStatus status,
        String errorCode,
        String errorMessage,
        BigDecimal missingQuantity,
        BigDecimal missingAmount,
        LocalDateTime processedAt
) {
}
