package ru.moysklad.intern.camunda.order.step;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.ProcessVariables;
import ru.moysklad.intern.entity.order.OrderProcessState;
import ru.moysklad.intern.repo.OrderProcessRepository;
import ru.moysklad.remap_1_2.ApiClient;
import ru.moysklad.remap_1_2.entities.documents.Demand;
import ru.moysklad.remap_1_2.utils.ApiClientException;

import java.util.UUID;

/**
 * КОМПЕНСАЦИЯ
 */
@Component
public class CancelDemandDraftStep {

    private static final Logger log = LoggerFactory.getLogger(CancelDemandDraftStep.class);

    private final ApiClient apiClient;
    private final OrderProcessRepository orderProcessRepository;

    public CancelDemandDraftStep(ApiClient apiClient, OrderProcessRepository orderProcessRepository) {
        this.apiClient = apiClient;
        this.orderProcessRepository = orderProcessRepository;
    }

    public void cancelDemand(DelegateExecution execution) throws Exception {
        UUID orderId = OrderStepUtils.extractOrderId(execution);
        log.info("Компенсация: Удаление черновика отгрузки для заказа {}", orderId);

        String demandIdStr = (String) execution.getVariable(ProcessVariables.DEMAND_ID);
        OrderProcessState state = orderProcessRepository.findById(orderId).orElse(null);
        if ((demandIdStr == null || demandIdStr.isBlank()) && state != null && state.getDemandId() != null) {
            demandIdStr = state.getDemandId().toString();
        }

        if (demandIdStr != null && !demandIdStr.isBlank()) {
            try {
                // проверяем, что отгрузка не проведена (applicable != true)
                Demand demand = apiClient.entity().demand().get(demandIdStr);
                if (demand != null && !Boolean.TRUE.equals(demand.getApplicable())) {
                    apiClient.entity().demand().delete(demandIdStr);
                    log.info("Черновик отгрузки {} успешно удален", demandIdStr);
                } else if (demand != null && Boolean.TRUE.equals(demand.getApplicable())) {
                    log.warn("Отгрузка {} уже проведена, удаление невозможно", demandIdStr);
                }
            } catch (ApiClientException e) {
                if (e.getStatusCode() == 404) {
                    log.info("Отгрузка {} уже не существует", demandIdStr);
                } else {
                    log.error("Сбой при удалении черновика отгрузки {}: HTTP {}", demandIdStr, e.getStatusCode(), e);
                    throw e;
                }
            }
        }

        if (state != null) {
            state.setDemandId(null);
            orderProcessRepository.save(state);
        }

        execution.removeVariable(ProcessVariables.DEMAND_ID);
    }
}
