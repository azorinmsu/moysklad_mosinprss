package ru.moysklad.intern.stockchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import ru.moysklad.intern.dto.kafka.StockChangeCommand;
import ru.moysklad.intern.entity.processing.ProcessingCommandType;
import ru.moysklad.intern.entity.processing.ProcessingRequest;
import ru.moysklad.intern.entity.processing.ProcessingRequestStatus;
import ru.moysklad.intern.repo.ProcessingRequestRepository;

@Component
public class StockChangeKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockChangeKafkaConsumer.class);

    private final ProcessingRequestRepository processingRequestRepository;
    private final StockChangeProcessLauncher processLauncher;
    private final ObjectMapper objectMapper;

    public StockChangeKafkaConsumer(
            ProcessingRequestRepository processingRequestRepository,
            StockChangeProcessLauncher processLauncher,
            ObjectMapper objectMapper
    ) {
        this.processingRequestRepository = processingRequestRepository;
        this.processLauncher = processLauncher;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.stock-commands}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void consume(StockChangeCommand command) {
        ProcessingRequest request = processingRequestRepository.findByRequestId(command.requestId())
                .orElseGet(() -> createFromCommand(command));

        if (request.getStatus() != ProcessingRequestStatus.PENDING) {
            log.info(
                    "Skip StockChange command requestId={} status={}",
                    command.requestId(),
                    request.getStatus()
            );
            return;
        }

        request.setStatus(ProcessingRequestStatus.PROCESSING);
        processingRequestRepository.save(request);
        processLauncher.launch(command);
    }

    private ProcessingRequest createFromCommand(StockChangeCommand command) {
        ProcessingRequest request = new ProcessingRequest();
        request.setRequestId(command.requestId());
        request.setCommandType(ProcessingCommandType.STOCK_CHANGE);
        request.setStatus(ProcessingRequestStatus.PENDING);
        request.setCorrelationId(command.correlationId() != null ? command.correlationId() : command.requestId());
        request.setPayload(toJson(command));
        return processingRequestRepository.save(request);
    }

    private String toJson(StockChangeCommand command) {
        try {
            return objectMapper.writeValueAsString(command);
        } catch (JacksonException e) {
            throw new IllegalStateException("Не удалось сериализовать StockChangeCommand", e);
        }
    }
}
