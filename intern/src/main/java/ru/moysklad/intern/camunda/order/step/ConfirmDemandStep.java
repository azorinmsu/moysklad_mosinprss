package ru.moysklad.intern.camunda.order.step;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessErrors;
import ru.moysklad.intern.camunda.ProcessVariables;
import ru.moysklad.intern.entity.order.OrderProcessStage;
import ru.moysklad.intern.entity.order.OrderProcessState;
import ru.moysklad.intern.repo.OrderProcessRepository;
import ru.moysklad.remap_1_2.ApiClient;
import ru.moysklad.remap_1_2.entities.documents.Demand;
import ru.moysklad.remap_1_2.utils.ApiClientException;

import java.util.UUID;

/**
 * ШАГ 7
 */
@Component
public class ConfirmDemandStep {

    private static final Logger log = LoggerFactory.getLogger(ConfirmDemandStep.class);

    private final ApiClient apiClient;
    private final OrderProcessRepository orderProcessRepository;

    public ConfirmDemandStep(ApiClient apiClient, OrderProcessRepository orderProcessRepository) {
        this.apiClient = apiClient;
        this.orderProcessRepository = orderProcessRepository;
    }

    public void confirmDemand(DelegateExecution execution) throws Exception {
        UUID orderId = OrderStepUtils.extractOrderId(execution);
        // этап 7
        log.info("подтверждение (проведение) отгрузки — точка невозврата для заказа {}", orderId);

        String demandIdStr = (String) execution.getVariable(ProcessVariables.DEMAND_ID);
        OrderProcessState state = orderProcessRepository.findById(orderId).orElse(null);
        if ((demandIdStr == null || demandIdStr.isBlank()) && state != null && state.getDemandId() != null) {
            demandIdStr = state.getDemandId().toString();
        }

        if (demandIdStr == null || demandIdStr.isBlank()) {
            throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Идентификатор отгрузки не найден для проведения");
        }

        // проводим отгрузку (applicable = true)
        Demand update = new Demand();
        update.setApplicable(true);

        try {
            apiClient.entity().demand().update(demandIdStr, update);
            log.info("Отгрузка {} успешно проведена (applicable=true)", demandIdStr);
        } catch (ApiClientException e) {
            if (e.getStatusCode() == 404 || e.getStatusCode() == 400) {
                throw new BpmnError(ProcessErrors.BUSINESS_ERROR,
                        "Ошибка при проведении отгрузки " + demandIdStr + ": " + e.getMessage());
            }
            log.error("Технический сбой при проведении отгрузки {}: HTTP {}", demandIdStr, e.getStatusCode(), e);
            throw e;
        }

        // фиксируем точку невозврата (irreversible = true)
        if (state == null) {
            state = new OrderProcessState();
            state.setOrderId(orderId);
            state.setProcessInstanceId(execution.getProcessInstanceId());
        }
        state.setIrreversible(true);
        state.setCurrentStage(OrderProcessStage.CONFIRM_DEMAND);
        orderProcessRepository.save(state);

        execution.setVariable(ProcessVariables.IRREVERSIBLE, true);
        execution.setVariable(ProcessVariables.CURRENT_STAGE, OrderProcessStage.CONFIRM_DEMAND.name());
        log.info("Точка невозврата пройдена для заказа {}", orderId);
    }
}
