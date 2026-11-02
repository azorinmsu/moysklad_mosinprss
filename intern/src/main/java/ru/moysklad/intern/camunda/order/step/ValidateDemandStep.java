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
import ru.moysklad.remap_1_2.entities.documents.CustomerOrder;
import ru.moysklad.remap_1_2.entities.documents.Demand;
import ru.moysklad.remap_1_2.entities.documents.positions.CustomerOrderDocumentPosition;
import ru.moysklad.remap_1_2.entities.documents.positions.DemandDocumentPosition;
import ru.moysklad.remap_1_2.utils.ApiClientException;
import ru.moysklad.remap_1_2.utils.params.ExpandParam;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
/**
 * ШАГ 6
 * */
@Component
public class ValidateDemandStep {

    private static final Logger log = LoggerFactory.getLogger(ValidateDemandStep.class);

    private final ApiClient apiClient;
    private final OrderProcessRepository orderProcessRepository;

    public ValidateDemandStep(ApiClient apiClient, OrderProcessRepository orderProcessRepository) {
        this.apiClient = apiClient;
        this.orderProcessRepository = orderProcessRepository;
    }

    public void validateDemand(DelegateExecution execution) throws Exception {
        UUID orderId = OrderStepUtils.extractOrderId(execution);
        log.info("проверка отгрузки для заказа {}", orderId);

        String demandIdStr = (String) execution.getVariable(ProcessVariables.DEMAND_ID);
        OrderProcessState state = orderProcessRepository.findById(orderId).orElse(null);
        if ((demandIdStr == null || demandIdStr.isBlank()) && state != null && state.getDemandId() != null) {
            demandIdStr = state.getDemandId().toString();
        }

        if (demandIdStr == null || demandIdStr.isBlank()) {
            throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Идентификатор отгрузки не найден для заказа: " + orderId);
        }

        // загружаем заказ с позициями
        CustomerOrder order;
        try {
            order = apiClient.entity().customerorder().get(orderId.toString(), ExpandParam.expand("positions"));
        } catch (ApiClientException e) {
            if (e.getStatusCode() == 404 || e.getStatusCode() == 400) {
                throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Заказ не найден: " + orderId);
            }
            throw e;
        }

        // загружаем отгрузку с позициями
        Demand demand;
        try {
            demand = apiClient.entity().demand().get(demandIdStr, ExpandParam.expand("positions"));
        } catch (ApiClientException e) {
            if (e.getStatusCode() == 404 || e.getStatusCode() == 400) {
                throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Созданная отгрузка " + demandIdStr + " не найдена");
            }
            throw e;
        }

        if (demand == null) {
            throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Отгрузка не найдена: " + demandIdStr);
        }

        // проверяем связь отгрузки с заказом
        if (demand.getCustomerOrder() == null) {
            throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Отгрузка " + demandIdStr + " не связана с заказом");
        }
        String linkedOrderId = OrderStepUtils.extractIdFromHref(
                demand.getCustomerOrder().getMeta() != null ? demand.getCustomerOrder().getMeta().getHref() : null);
        if (linkedOrderId != null && !linkedOrderId.equalsIgnoreCase(orderId.toString())) {
            throw new BpmnError(ProcessErrors.BUSINESS_ERROR,
                    "Отгрузка " + demandIdStr + " связана с другим заказом: " + linkedOrderId);
        }

        // сверяем позиции заказа и позиции отгрузки
        Map<String, Double> orderQuantities = new HashMap<>();
        if (order.getPositions() != null && order.getPositions().getRows() != null) {
            for (CustomerOrderDocumentPosition pos : order.getPositions().getRows()) {
                String prodId = OrderStepUtils.extractProductId(pos.getAssortment());
                double qty = pos.getQuantity() != null ? pos.getQuantity() : 0.0;
                orderQuantities.merge(prodId, qty, Double::sum);
            }
        }

        Map<String, Double> demandQuantities = new HashMap<>();
        if (demand.getPositions() != null && demand.getPositions().getRows() != null) {
            for (DemandDocumentPosition pos : demand.getPositions().getRows()) {
                String prodId = OrderStepUtils.extractProductId(pos.getAssortment());
                double qty = pos.getQuantity() != null ? pos.getQuantity() : 0.0;
                demandQuantities.merge(prodId, qty, Double::sum);
            }
        }

        if (!orderQuantities.equals(demandQuantities)) {
            log.warn("Несоответствие позиций между заказом {} и отгрузкой {}. Заказ: {}, Отгрузка: {}",
                    orderId, demandIdStr, orderQuantities, demandQuantities);
            throw new BpmnError(ProcessErrors.BUSINESS_ERROR,
                    "Несоответствие позиций отгрузки заказу: ожидалось " + orderQuantities + ", получено " + demandQuantities);
        }

        if (state != null) {
            state.setCurrentStage(OrderProcessStage.VALIDATE_DEMAND);
            orderProcessRepository.save(state);
        }
        execution.setVariable(ProcessVariables.CURRENT_STAGE, OrderProcessStage.VALIDATE_DEMAND.name());
        log.info("Отгрузка {} успешно проверена и соответствует заказу {}", demandIdStr, orderId);
    }
}
