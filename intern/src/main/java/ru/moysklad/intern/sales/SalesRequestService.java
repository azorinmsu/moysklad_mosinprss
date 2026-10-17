package ru.moysklad.intern.sales;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.dto.kafka.SalesCommand;
import ru.moysklad.intern.repo.ProcessingRequestRepository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Приём заявок на реализацию товара.
 *
 * <p><b>TODO (студент):</b> реализовать валидацию, генерацию requestId,
 * сохранение заявки и публикацию {@link SalesCommand} через {@link SalesKafkaProducer}.
 */
@Service
public class SalesRequestService {

    // TODO(student): внедрить зависимости (репозиторий, SalesKafkaProducer, ObjectMapper и т.д.)
    private final ProcessingRequestRepository processingRequestRepository; // вроде с репозиторием угадал
    private final SalesKafkaProducer salesKafkaProducer;
    private final ObjectMapper objectMapper;

    public SalesRequestService(ProcessingRequestRepository processingRequestRepository, SalesKafkaProducer salesKafkaProducer, ObjectMapper objectMapper) {
        this.processingRequestRepository = processingRequestRepository;
        this.salesKafkaProducer = salesKafkaProducer;
        this.objectMapper = objectMapper;
    }

    /**
     * TODO(student): принять заявку на реализацию и вернуть requestId.
     */
    @Transactional
    public UUID submit(SalesCommand request) {
        validate(request);

        UUID requestId = UUID.randomUUID();
        UUID correlationId = request.correlationId() != null ? request.correlationId() : requestId;
        // TODO: доделать этот сервис

        return requestId;
    }

    public void validate(SalesCommand request) {
        if (request == null)
            throw new IllegalArgumentException("Заявка не задана");

        if (request.productId() == null)
            throw new IllegalArgumentException("ID товара обязателен");

        if (request.storeId() == null)
            throw new IllegalArgumentException("ID магазина обязателен");

        if (request.quantity() == null)
            // если я правильно понимаю, о чем речь
            throw new IllegalArgumentException("Отсутствует количество товара");
        if (request.quantity().compareTo(BigDecimal.ZERO) == 0)
            throw new IllegalArgumentException("Отсутствует количество товара");

        if (request.availableAmount() == null)
            // если я правильно понимаю, о чем речь х2
            throw new IllegalArgumentException("Отсутствует доступное количество");
        if (request.availableAmount().compareTo(BigDecimal.ZERO) == 0)
            throw new IllegalArgumentException("Отсутствует доступное количество");

        if (request.requestedAt() == null)
            throw new IllegalArgumentException("Отсутствует информация о времени запроса");
    }

    private String toJson(SalesCommand command) {
        try {
            return objectMapper.writeValueAsString(command);
        } catch (JacksonException e) {
            throw new IllegalStateException("Ошибка сериализации SalesCommand: ", e);
        }
    }
}
