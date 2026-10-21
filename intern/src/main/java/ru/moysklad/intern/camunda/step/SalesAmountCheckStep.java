package ru.moysklad.intern.camunda.step;

import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessContext;
import ru.moysklad.intern.dto.kafka.SalesCommand;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.aggregate.Price;
import ru.moysklad.intern.entity.processing.ProcessingRequest;
import ru.moysklad.intern.repo.PriceRepository;
import ru.moysklad.intern.repo.ProcessingRequestRepository;
import ru.moysklad.intern.repo.ProductRepository;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

/**
 * <b>TODO (студент):</b> сравнить доступную сумму с quantity × salePrice.
 * При нехватке: errorCode {@code INSUFFICIENT_FUNDS}, missingAmount = required - available.
 */
@Component
public class SalesAmountCheckStep {

    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;
    private final ProcessingRequestRepository processingRequestRepository;
    private final ObjectMapper objectMapper;

    public SalesAmountCheckStep(
            ProductRepository productRepository,
            PriceRepository priceRepository,
            ProcessingRequestRepository processingRequestRepository,
            ObjectMapper objectMapper
    ) {
        this.productRepository = productRepository;
        this.priceRepository = priceRepository;
        this.processingRequestRepository = processingRequestRepository;
        this.objectMapper = objectMapper;
    }

    public void execute(ProcessContext context) {
        SalesCommand command = extractCommand(context);

        BigDecimal salePrice = productRepository.findById(command.productId())
                .map(Product::getPrice)
                .orElse(null);

        if (salePrice == null) {
            salePrice = priceRepository.findByProduct_Id(command.productId()).stream()
                    .map(Price::getUnitPrice)
                    .findFirst()
                    .orElse(null);
        }

        if (salePrice == null) {
            context.fail("PRICE_NOT_FOUND", "Не удалось определить цену товара для продажи: " + command.productId());
            return;
        }

        BigDecimal requiredAmount = command.quantity().multiply(salePrice);
        if (command.availableAmount().compareTo(requiredAmount) < 0) {
            BigDecimal missingAmount = requiredAmount.subtract(command.availableAmount());
            context.fail(
                    "INSUFFICIENT_FUNDS",
                    String.format("Недостаточно средств. Требуется: %s, доступно: %s", requiredAmount, command.availableAmount()),
                    null,
                    missingAmount
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
