package ru.moysklad.intern.camunda.step;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.dto.kafka.SalesCommand;
import ru.moysklad.intern.entity.processing.ProcessingRequest;
import ru.moysklad.intern.entity.processing.ProcessingRequestStatus;
import ru.moysklad.intern.repo.ProcessingRequestRepository;
import ru.moysklad.intern.sales.SalesProcessLauncher;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

/**
 * Выбор и захват PENDING-заявок реализации. Вызывается timer-процессом, не Kafka consumer.
 *
 * <p><b>TODO (студент):</b> забрать заявку так, чтобы при нескольких инстансах она не стартовала дважды.
 * Нужны идемпотентность, защита от повторной обработки и восстановление после падения.
 * Здесь намеренно пустая реализация: timer не должен падать, пока шаг не написан.
 */
@Component
public class SalesRequestClaimStep {

    private static final Logger log = LoggerFactory.getLogger(SalesRequestClaimStep.class);

    private final ProcessingRequestRepository processingRequestRepository;
    private final SalesProcessLauncher salesProcessLauncher;
    private final ObjectMapper objectMapper;

    public SalesRequestClaimStep(
            ProcessingRequestRepository processingRequestRepository,
            SalesProcessLauncher salesProcessLauncher,
            ObjectMapper objectMapper
    ) {
        this.processingRequestRepository = processingRequestRepository;
        this.salesProcessLauncher = salesProcessLauncher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void claimAndStart() {
        Optional<ProcessingRequest> claimedOpt = processingRequestRepository.claimNextPendingSale();
        if (claimedOpt.isEmpty()) {
            return;
        }

        ProcessingRequest request = claimedOpt.get();
        request.setStatus(ProcessingRequestStatus.PROCESSING);
        processingRequestRepository.saveAndFlush(request);

        try {
            SalesCommand command = objectMapper.readValue(request.getPayload(), SalesCommand.class);
            salesProcessLauncher.launch(command);
        } catch (Exception e) {
            log.error("Не удалось запустить процесс реализации для requestId={}", request.getRequestId(), e);
            request.setStatus(ProcessingRequestStatus.FAILED);
            processingRequestRepository.save(request);
        }
    }
}
