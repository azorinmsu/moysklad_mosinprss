package ru.moysklad.intern.sales;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.moysklad.intern.dto.kafka.SalesCommand;
import ru.moysklad.intern.entity.processing.ProcessingCommandType;
import ru.moysklad.intern.entity.processing.ProcessingRequest;
import ru.moysklad.intern.entity.processing.ProcessingRequestStatus;
import ru.moysklad.intern.repo.ProcessingRequestRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Приём заявок на реализацию товара.
 */
@Service
public class SalesRequestService {

    private final ProcessingRequestRepository processingRequestRepository;
    private final SalesKafkaProducer salesKafkaProducer;
    private final ObjectMapper objectMapper;

    public SalesRequestService(
            ProcessingRequestRepository processingRequestRepository,
            SalesKafkaProducer salesKafkaProducer,
            ObjectMapper objectMapper
    ) {
        this.processingRequestRepository = processingRequestRepository;
        this.salesKafkaProducer = salesKafkaProducer;
        this.objectMapper = objectMapper;
    }

    /**
     * Принимает заявку на реализацию, сохраняет её со статусом PENDING
     * и публикует в Kafka после успешного коммита транзакции.
     *
     * @return сгенерированный requestId
     */
    @Transactional
    public UUID submit(SalesCommand request) {
        validate(request);

        UUID requestId = request.requestId() != null ? request.requestId() : UUID.randomUUID();
        UUID correlationId = request.correlationId() != null ? request.correlationId() : requestId;
        SalesCommand command = new SalesCommand(
                requestId,
                request.productId(),
                request.storeId(),
                request.quantity(),
                request.availableAmount(),
                request.requestedAt(),
                correlationId
        );

        ProcessingRequest entity = new ProcessingRequest();
        entity.setRequestId(requestId);
        entity.setCommandType(ProcessingCommandType.SALES);
        entity.setStatus(ProcessingRequestStatus.PENDING);
        entity.setCorrelationId(correlationId);
        entity.setPayload(toJson(command));
        processingRequestRepository.save(entity);

        publishAfterCommit(command);
        return requestId;
    }

    private void publishAfterCommit(SalesCommand command) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            salesKafkaProducer.publish(command);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                salesKafkaProducer.publish(command);
            }
        });
    }

    public void validate(SalesCommand request) {
        if (request == null) {
            throw new IllegalArgumentException("Заявка не задана");
        }
        if (request.productId() == null) {
            throw new IllegalArgumentException("ID товара обязателен");
        }
        if (request.storeId() == null) {
            throw new IllegalArgumentException("ID магазина обязателен");
        }
        if (request.quantity() == null) {
            throw new IllegalArgumentException("Количество товара обязательно");
        }
        if (request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Количество товара должно быть больше нуля");
        }
        if (request.availableAmount() == null) {
            throw new IllegalArgumentException("Доступная сумма обязательна");
        }
        if (request.availableAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Доступная сумма должна быть больше нуля");
        }
        if (request.requestedAt() == null) {
            throw new IllegalArgumentException("Дата и время запроса обязательны");
        }
    }

    private String toJson(SalesCommand command) {
        try {
            return objectMapper.writeValueAsString(command);
        } catch (JacksonException e) {
            throw new IllegalStateException("Ошибка сериализации SalesCommand: ", e);
        }
    }
}
