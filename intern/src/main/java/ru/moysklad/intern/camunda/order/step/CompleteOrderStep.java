package ru.moysklad.intern.camunda.order.step;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessVariables;
import ru.moysklad.intern.entity.order.OrderProcessStage;
import ru.moysklad.intern.entity.order.OrderProcessState;
import ru.moysklad.intern.entity.order.OrderProcessStatus;
import ru.moysklad.intern.repo.OrderProcessRepository;

import java.util.UUID;

/**
 * ШАГ 8, ФИНАЛ
 */
@Component
public class CompleteOrderStep {

    private static final Logger log = LoggerFactory.getLogger(CompleteOrderStep.class);

    private final OrderProcessRepository orderProcessRepository;

    public CompleteOrderStep(OrderProcessRepository orderProcessRepository) {
        this.orderProcessRepository = orderProcessRepository;
    }

    public void completeOrder(DelegateExecution execution) {
        UUID orderId = OrderStepUtils.extractOrderId(execution);
        // этап 8
        log.info("завершение обработки заказа {}", orderId);

        OrderProcessState state = orderProcessRepository.findById(orderId).orElse(null);
        if (state == null) {
            state = new OrderProcessState();
            state.setOrderId(orderId);
            state.setProcessInstanceId(execution.getProcessInstanceId());
        }

        state.setStatus(OrderProcessStatus.COMPLETED);
        state.setCurrentStage(OrderProcessStage.COMPLETE_ORDER);
        orderProcessRepository.save(state);

        execution.setVariable(ProcessVariables.FINAL_STATUS, OrderProcessStatus.COMPLETED.name());
        execution.setVariable(ProcessVariables.CURRENT_STAGE, OrderProcessStage.COMPLETE_ORDER.name());
        log.info("обработка заказа {} успешно завершена со статусом COMPLETED", orderId);
    }
}
