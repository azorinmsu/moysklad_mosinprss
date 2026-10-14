package ru.moysklad.intern.camunda;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.dto.kafka.ProcessingResultMessage;
import ru.moysklad.intern.entity.processing.OutboxEvent;
import ru.moysklad.intern.entity.processing.OutboxEventStatus;
import ru.moysklad.intern.entity.processing.ProcessingRequest;
import ru.moysklad.intern.entity.processing.ProcessingRequestStatus;
import ru.moysklad.intern.entity.processing.ProcessingResultStatus;
import ru.moysklad.intern.kafka.KafkaTopicProperties;
import ru.moysklad.intern.repo.OutboxEventRepository;
import ru.moysklad.intern.repo.ProcessingRequestRepository;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Фиксирует исход процесса в заявке и transactional outbox.
 * Отдельный publisher отправит событие в Kafka на следующем этапе.
 */
@Service
public class ProcessOutcomeRecorder {

    private final ProcessingRequestRepository processingRequestRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTopicProperties topics;
    private final ObjectMapper objectMapper;

    public ProcessOutcomeRecorder(
            ProcessingRequestRepository processingRequestRepository,
            OutboxEventRepository outboxEventRepository,
            KafkaTopicProperties topics,
            ObjectMapper objectMapper
    ) {
        this.processingRequestRepository = processingRequestRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.topics = topics;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordSuccess(DelegateExecution execution) {
        record(execution, ProcessingResultStatus.SUCCESS, ProcessingRequestStatus.COMPLETED);
    }

    @Transactional
    public void recordFailure(DelegateExecution execution) {
        record(execution, ProcessingResultStatus.ERROR, ProcessingRequestStatus.FAILED);
    }

    private void record(
            DelegateExecution execution,
            ProcessingResultStatus resultStatus,
            ProcessingRequestStatus requestStatus
    ) {
        execution.setVariable(ProcessVariables.STATUS, resultStatus.name());
        String requestIdValue = stringVariable(execution, ProcessVariables.REQUEST_ID);
        if (requestIdValue == null) {
            return;
        }
        UUID requestId = UUID.fromString(requestIdValue);
        processingRequestRepository.findByRequestId(requestId).ifPresent(request -> updateRequest(request, requestStatus));

        ProcessingResultMessage message = new ProcessingResultMessage(
                requestId,
                resultStatus,
                stringVariable(execution, ProcessVariables.ERROR_CODE),
                stringVariable(execution, ProcessVariables.ERROR_MESSAGE),
                decimalVariable(execution, ProcessVariables.MISSING_QUANTITY),
                decimalVariable(execution, ProcessVariables.MISSING_AMOUNT),
                LocalDateTime.now()
        );

        OutboxEvent event = new OutboxEvent();
        event.setTopic(topics.getProcessingResults());
        event.setEventKey(requestId.toString());
        event.setPayload(objectMapper.writeValueAsString(message));
        event.setStatus(OutboxEventStatus.PENDING);
        outboxEventRepository.save(event);
    }

    private void updateRequest(ProcessingRequest request, ProcessingRequestStatus requestStatus) {
        request.setStatus(requestStatus);
        processingRequestRepository.save(request);
    }

    private String stringVariable(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        return value == null ? null : value.toString();
    }

    private BigDecimal decimalVariable(DelegateExecution execution, String name) {
        String value = stringVariable(execution, name);
        return value == null || value.isBlank() ? null : new BigDecimal(value);
    }
}
