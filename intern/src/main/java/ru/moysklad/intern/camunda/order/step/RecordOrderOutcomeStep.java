package ru.moysklad.intern.camunda.order.step;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessVariables;
import ru.moysklad.intern.entity.order.OrderProcessState;
import ru.moysklad.intern.entity.order.OrderProcessStatus;
import ru.moysklad.intern.repo.OrderProcessRepository;

import java.util.UUID;

// для записи неуспешных исходов
@Component
public class RecordOrderOutcomeStep {

    private static final Logger log = LoggerFactory.getLogger(RecordOrderOutcomeStep.class);

    private final OrderProcessRepository orderProcessRepository;

    public RecordOrderOutcomeStep(OrderProcessRepository orderProcessRepository) {
        this.orderProcessRepository = orderProcessRepository;
    }

    public void recordOutcome(DelegateExecution execution) {
        UUID orderId = OrderStepUtils.extractOrderId(execution);
        String outcomeStr = (String) execution.getVariable(ProcessVariables.OUTCOME);
        log.info("фиксация исхода заказа {}: {}", orderId, outcomeStr);

        OrderProcessStatus status = OrderProcessStatus.FAILED;
        if (outcomeStr != null && !outcomeStr.isBlank()) {
            try {
                status = OrderProcessStatus.valueOf(outcomeStr.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Неизвестный исход процесса: {}, используется FAILED", outcomeStr);
            }
        }

        String errorReason = (String) execution.getVariable(ProcessVariables.ERROR_REASON);
        if (errorReason == null || errorReason.isBlank()) {
            errorReason = (String) execution.getVariable(ProcessVariables.ERROR_MESSAGE);
        }

        OrderProcessState state = orderProcessRepository.findById(orderId).orElse(null);
        if (state == null) {
            state = new OrderProcessState();
            state.setOrderId(orderId);
            state.setProcessInstanceId(execution.getProcessInstanceId());
        }

        state.setStatus(status);
        if (errorReason != null && !errorReason.isBlank()) {
            state.setErrorReason(errorReason);
        }
        orderProcessRepository.save(state);

        execution.setVariable(ProcessVariables.FINAL_STATUS, status.name());
        log.info("Для заказа {} успешно зафиксирован статус {}", orderId, status);
    }
}
