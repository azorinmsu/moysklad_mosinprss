package ru.moysklad.intern.sales;

import org.springframework.stereotype.Service;
import ru.moysklad.intern.dto.kafka.SalesCommand;

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

    /**
     * TODO(student): принять заявку на реализацию и вернуть requestId.
     */
    public UUID submit(SalesCommand request) {
        throw new UnsupportedOperationException("TODO: реализовать SalesRequestService.submit");
    }
}
