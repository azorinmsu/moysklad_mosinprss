package ru.moysklad.intern.camunda.step;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.camunda.ProcessContext;
import ru.moysklad.intern.dto.kafka.SalesCommand;
import ru.moysklad.intern.entity.aggregate.AggregateStock;
import ru.moysklad.intern.entity.aggregate.StockMovement;
import ru.moysklad.intern.entity.aggregate.StockMovementOperationType;
import ru.moysklad.intern.entity.processing.ProcessingRequest;
import ru.moysklad.intern.repo.AggregateStockRepository;
import ru.moysklad.intern.repo.ProcessingRequestRepository;
import ru.moysklad.intern.repo.StockMovementRepository;
import ru.moysklad.intern.service.aggregate.PriceService;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <b>TODO (студент):</b> списать товар и пересчитать себестоимость.
 * Предпочтительно общая операция с AggregateStock и Price, а не второй независимый алгоритм.
 */
@Component
public class SalesWriteOffStep {

    private final AggregateStockRepository aggregateStockRepository;
    private final StockMovementRepository stockMovementRepository;
    private final PriceService priceService;
    private final ProcessingRequestRepository processingRequestRepository;
    private final ObjectMapper objectMapper;

    public SalesWriteOffStep(
            AggregateStockRepository aggregateStockRepository,
            StockMovementRepository stockMovementRepository,
            PriceService priceService,
            ProcessingRequestRepository processingRequestRepository,
            ObjectMapper objectMapper
    ) {
        this.aggregateStockRepository = aggregateStockRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.priceService = priceService;
        this.processingRequestRepository = processingRequestRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void execute(ProcessContext context) {
        SalesCommand command = extractCommand(context);

        List<AggregateStock> availableStocks = aggregateStockRepository
                .findAvailableForUpdate(command.productId(), command.storeId());

        BigDecimal totalAvailable = availableStocks.stream()
                .map(AggregateStock::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAvailable.compareTo(command.quantity()) < 0) {
            BigDecimal missing = command.quantity().subtract(totalAvailable);
            context.fail(
                    "INSUFFICIENT_STOCK",
                    String.format("Недостаточно остатка под блокировкой. Доступно: %s, требуется: %s", totalAvailable, command.quantity()),
                    missing,
                    null
            );
            return;
        }

        BigDecimal remaining = command.quantity();
        LocalDateTime moment = command.requestedAt() != null ? command.requestedAt() : LocalDateTime.now();

        for (AggregateStock stock : availableStocks) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal availableInStock = stock.getQuantity();
            BigDecimal toDeduct = availableInStock.min(remaining);

            stock.setQuantity(availableInStock.subtract(toDeduct));
            aggregateStockRepository.save(stock);

            priceService.recalculateTotalCost(stock.getId());

            StockMovement movement = new StockMovement();
            movement.setDocumentId(command.requestId());
            movement.setProduct(stock.getProduct());
            movement.setStore(stock.getStore());
            movement.setConsignment(stock.getConsignment());
            movement.setQuantityDelta(toDeduct.negate());
            movement.setMoment(moment);
            movement.setOperationType(StockMovementOperationType.SALES);
            stockMovementRepository.save(movement);

            remaining = remaining.subtract(toDeduct);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            context.fail("INSUFFICIENT_STOCK", "Не удалось списать требуемое количество", remaining, null);
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
