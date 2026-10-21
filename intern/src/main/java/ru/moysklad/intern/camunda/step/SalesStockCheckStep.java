package ru.moysklad.intern.camunda.step;

import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessContext;
import ru.moysklad.intern.dto.kafka.SalesCommand;
import ru.moysklad.intern.entity.processing.ProcessingRequest;
import ru.moysklad.intern.repo.AggregateStockRepository;
import ru.moysklad.intern.repo.ProcessingRequestRepository;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

/**
 * <b>TODO (студент):</b> проверить доступное количество для заявки на реализацию.
 * При нехватке: errorCode {@code INSUFFICIENT_STOCK}, missingQuantity = requested - available.
 */
@Component
public class SalesStockCheckStep {

    private final AggregateStockRepository aggregateStockRepository;
    private final ProcessingRequestRepository processingRequestRepository;
    private final ObjectMapper objectMapper;

    public SalesStockCheckStep(
            AggregateStockRepository aggregateStockRepository,
            ProcessingRequestRepository processingRequestRepository,
            ObjectMapper objectMapper
    ) {
        this.aggregateStockRepository = aggregateStockRepository;
        this.processingRequestRepository = processingRequestRepository;
        this.objectMapper = objectMapper;
    }

    public void execute(ProcessContext context) {
        SalesCommand command = extractCommand(context);
        BigDecimal totalAvailable = aggregateStockRepository
                .sumQuantityByProductAndStore(command.productId(), command.storeId());
        if (totalAvailable == null) {
            totalAvailable = BigDecimal.ZERO;
        }

        if (totalAvailable.compareTo(command.quantity()) < 0) {
            BigDecimal missing = command.quantity().subtract(totalAvailable.max(BigDecimal.ZERO));
            context.fail(
                    "INSUFFICIENT_STOCK",
                    String.format("Недостаточно товара на складе. Доступно: %s, требуется: %s", totalAvailable, command.quantity()),
                    missing,
                    null
            );
        }
    }

    private SalesCommand extractCommand(ProcessContext context) {
        try {
            Object json = context.execution().getVariable("salesCommandJson");
            if (json != null && !json.toString().isBlank()) {
                return objectMapper.readValue(json.toString(), SalesCommand.class);
            }
            ProcessingRequest request = processingRequestRepository.findByRequestId(context.requestId())
                    .orElseThrow(() -> new IllegalStateException("Заявка не найдена: " + context.requestId()));
            return objectMapper.readValue(request.getPayload(), SalesCommand.class);
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось получить SalesCommand из контекста или БД", e);
        }
    }
}
