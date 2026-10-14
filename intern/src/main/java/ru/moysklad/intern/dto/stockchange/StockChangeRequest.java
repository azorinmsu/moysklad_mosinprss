package ru.moysklad.intern.dto.stockchange;

import ru.moysklad.intern.dto.kafka.StockChangePositionDto;
import ru.moysklad.intern.entity.operation.DocumentsType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Входящая заявка на изменение остатков и себестоимости по документу.
 */
public record StockChangeRequest(
        UUID documentId,
        DocumentsType documentType,
        LocalDateTime moment,
        List<StockChangePositionDto> positions,
        UUID correlationId
) {
}
