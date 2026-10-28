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

/**
 * ФИКСАЦИЯ ОТМЕНЫ
 */

@Component
public class HandleCancellationStep {

    private static final Logger log = LoggerFactory.getLogger(HandleCancellationStep.class);

    private final OrderProcessRepository orderProcessRepository;

    public HandleCancellationStep(OrderProcessRepository orderProcessRepository) {
        this.orderProcessRepository = orderProcessRepository;
    }

    public void handleCancellation(DelegateExecution execution) {
        UUID orderId = OrderStepUtils.extractOrderId(execution);
        log.info("Фиксация отмены заказа {}", orderId);

        OrderProcessState state = orderProcessRepository.findById(orderId).orElse(null);
        if (state == null) {
            state = new OrderProcessState();
            state.setOrderId(orderId);
            state.setProcessInstanceId(execution.getProcessInstanceId());
        }

        state.setStatus(OrderProcessStatus.CANCELLED);
        orderProcessRepository.save(state);

        execution.setVariable(ProcessVariables.FINAL_STATUS, OrderProcessStatus.CANCELLED.name());
        log.info("Для заказа {} успешно зафиксирован статус CANCELLED", orderId);
    }
}
