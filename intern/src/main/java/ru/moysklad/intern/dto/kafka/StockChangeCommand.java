package ru.moysklad.intern.dto.kafka;

import ru.moysklad.intern.entity.operation.DocumentsType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record StockChangeCommand(
        UUID requestId,
        UUID documentId,
        DocumentsType documentType,
        LocalDateTime moment,
        List<StockChangePositionDto> positions,
        UUID correlationId
) {
}
