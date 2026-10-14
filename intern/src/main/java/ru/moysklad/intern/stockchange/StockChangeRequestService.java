package ru.moysklad.intern.stockchange;

import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.moysklad.intern.dto.kafka.StockChangeCommand;
import ru.moysklad.intern.dto.kafka.StockChangePositionDto;
import ru.moysklad.intern.dto.stockchange.StockChangeRequest;
import ru.moysklad.intern.entity.processing.ProcessingCommandType;
import ru.moysklad.intern.entity.processing.ProcessingRequest;
import ru.moysklad.intern.entity.processing.ProcessingRequestStatus;
import ru.moysklad.intern.repo.ProcessingRequestRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class StockChangeRequestService {

    private final ProcessingRequestRepository processingRequestRepository;
    private final StockChangeKafkaProducer stockChangeKafkaProducer;
    private final ObjectMapper objectMapper;

    public StockChangeRequestService(
            ProcessingRequestRepository processingRequestRepository,
            StockChangeKafkaProducer stockChangeKafkaProducer,
            ObjectMapper objectMapper
    ) {
        this.processingRequestRepository = processingRequestRepository;
        this.stockChangeKafkaProducer = stockChangeKafkaProducer;
        this.objectMapper = objectMapper;
    }

    /**
     * Принимает заявку, сохраняет её и публикует команду в Kafka после commit транзакции.
     *
     * @return сгенерированный requestId
     */
    @Transactional
    public UUID submit(StockChangeRequest request) {
        validate(request);

        UUID requestId = UUID.randomUUID();
        UUID correlationId = request.correlationId() != null ? request.correlationId() : requestId;

        StockChangeCommand command = new StockChangeCommand(
                requestId,
                request.documentId(),
                request.documentType(),
                request.moment(),
                List.copyOf(request.positions()),
                correlationId
        );

        ProcessingRequest entity = new ProcessingRequest();
        entity.setRequestId(requestId);
        entity.setCommandType(ProcessingCommandType.STOCK_CHANGE);
        entity.setStatus(ProcessingRequestStatus.PENDING);
        entity.setCorrelationId(correlationId);
        entity.setPayload(toJson(command));
        processingRequestRepository.save(entity);

        publishAfterCommit(command);
        return requestId;
    }

    private void publishAfterCommit(StockChangeCommand command) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            stockChangeKafkaProducer.publish(command);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                stockChangeKafkaProducer.publish(command);
            }
        });
    }

    private void validate(StockChangeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Заявка не задана");
        }
        if (request.documentId() == null) {
            throw new IllegalArgumentException("documentId обязателен");
        }
        if (request.documentType() == null) {
            throw new IllegalArgumentException("documentType обязателен");
        }
        if (request.moment() == null) {
            throw new IllegalArgumentException("moment обязателен");
        }
        if (request.positions() == null || request.positions().isEmpty()) {
            throw new IllegalArgumentException("positions не должны быть пустыми");
        }
        for (int i = 0; i < request.positions().size(); i++) {
            validatePosition(request.positions().get(i), i);
        }
    }

    private void validatePosition(StockChangePositionDto position, int index) {
        if (position == null) {
            throw new IllegalArgumentException("positions[" + index + "] не задана");
        }
        if (position.productId() == null) {
            throw new IllegalArgumentException("positions[" + index + "].productId обязателен");
        }
        if (position.storeId() == null) {
            throw new IllegalArgumentException("positions[" + index + "].storeId обязателен");
        }
        if (position.consignmentId() == null) {
            throw new IllegalArgumentException("positions[" + index + "].consignmentId обязателен");
        }
        if (position.quantity() == null) {
            throw new IllegalArgumentException("positions[" + index + "].quantity обязателен");
        }
        if (position.quantity().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("positions[" + index + "].quantity не должен быть нулевым");
        }
    }

    private String toJson(StockChangeCommand command) {
        try {
            return objectMapper.writeValueAsString(command);
        } catch (JacksonException e) {
            throw new IllegalStateException("Не удалось сериализовать StockChangeCommand", e);
        }
    }
}
