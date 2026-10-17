package ru.moysklad.intern.sales;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.dto.kafka.SalesCommand;
import ru.moysklad.intern.repo.ProcessingRequestRepository;
import tools.jackson.databind.ObjectMapper;

/**
 * Чтение команд реализации из топика {@code sales.commands}.
 *
 * <p><b>TODO (студент):</b>
 * <ul>
 *   <li>добавить {@code @KafkaListener} на topic {@code sales.commands};</li>
 *   <li>десериализовать {@link SalesCommand};</li>
 *   <li>обеспечить идемпотентность по requestId;</li>
 *   <li>сохранить заявку со статусом PENDING;</li>
 *   <li>не запускать Camunda из listener напрямую по таймеру —
 *       timer обрабатывает уже сохранённые PENDING-заявки.</li>
 * </ul>
 *
 * <p>Рабочую реализацию StockChangeKafkaConsumer не переносите сюда целиком:
 * у Sales другой жизненный цикл (PENDING → периодический захват).
 */
@Component
public class SalesKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(SalesKafkaConsumer.class);
    // TODO(student): зависимости и @KafkaListener
    private final ProcessingRequestRepository processingRequestRepository;
    private final SalesProcessLauncher processLauncher;
    private final ObjectMapper objectMapper;

    public SalesKafkaConsumer(ProcessingRequestRepository processingRequestRepository, SalesProcessLauncher processLauncher, ObjectMapper objectMapper) {
        this.processingRequestRepository = processingRequestRepository;
        this.processLauncher = processLauncher;
        this.objectMapper = objectMapper;
    }

    /**
     * TODO(student): обработать входящую SalesCommand.
     */
    @KafkaListener(
            topics = "${app.kafka.topics.sales-commands}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void consume(SalesCommand command) {
        throw new UnsupportedOperationException("TODO: реализовать SalesKafkaConsumer.consume");
    }
}
