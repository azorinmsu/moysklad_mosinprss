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
import ru.moysklad.remap_1_2.entities.documents.positions.CustomerOrderDocumentPosition;
import ru.moysklad.remap_1_2.utils.ApiClientException;
import ru.moysklad.remap_1_2.utils.params.ExpandParam;

import java.util.UUID;

/**
 * ШАГ 3
 */
@Component
public class ReserveStockStep {

    private static final Logger log = LoggerFactory.getLogger(ReserveStockStep.class);

    private final ApiClient apiClient;
    private final OrderProcessRepository orderProcessRepository;

    public ReserveStockStep(ApiClient apiClient, OrderProcessRepository orderProcessRepository) {
        this.apiClient = apiClient;
        this.orderProcessRepository = orderProcessRepository;
    }

    public void reserve(DelegateExecution execution) throws Exception {
        UUID orderId = OrderStepUtils.extractOrderId(execution);
        log.info("резервирование остатков для заказа {}", orderId);

        // загружаем заказ с позициями
        CustomerOrder order;
        try {
            order = apiClient.entity().customerorder().get(orderId.toString(), ExpandParam.expand("positions"));
        } catch (ApiClientException e) {
            if (e.getStatusCode() == 404) {
                log.warn("Заказ {} не найден в МойСклад (HTTP 404)", orderId);
                throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Заказ не найден в МойСклад: " + orderId);
            } else if (e.getStatusCode() == 400) {
                log.warn("Некорректный запрос к МойСклад для заказа {} (HTTP 400)", orderId);
                throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Некорректный запрос к МойСклад для заказа: " + orderId);
            }
            log.error("Временный технический сбой при получении заказа {}: HTTP {}", orderId, e.getStatusCode(), e);
            throw e;
        }

        if (order == null || order.getPositions() == null || order.getPositions().getRows() == null || order.getPositions().getRows().isEmpty()) {
            throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Заказ " + orderId + " не содержит позиций");
        }

        // выставляем reserve = quantity для каждой позиции
        for (CustomerOrderDocumentPosition pos : order.getPositions().getRows()) {
            Double quantity = pos.getQuantity();
            Double currentReserve = pos.getReserve();
            if (quantity == null || quantity <= 0) {
                continue;
            }
            if (currentReserve != null && currentReserve.equals(quantity)) {
                log.debug("Позиция {} уже имеет резерв {}", pos.getId(), currentReserve);
                continue;
            }

            try {
                CustomerOrderDocumentPosition updatePos = new CustomerOrderDocumentPosition();
                updatePos.setReserve(quantity);
                apiClient.entity().customerorder().updatePosition(orderId.toString(), pos.getId(), updatePos);
                log.info("Резерв {} установлен для позиции {} заказа {}", quantity, pos.getId(), orderId);
            } catch (ApiClientException e) {
                if (e.getStatusCode() == 404 || e.getStatusCode() == 400) {
                    throw new BpmnError(ProcessErrors.BUSINESS_ERROR,
                            "Ошибка при установке резерва для заказа " + orderId + ": " + e.getMessage());
                }
                log.error("Временный сбой при установке резерва для позиции {} заказа {}: HTTP {}",
                        pos.getId(), orderId, e.getStatusCode(), e);
                throw e;
            }
        }

        // обновляем состояние заказа в БД
        OrderProcessState state = orderProcessRepository.findById(orderId).orElse(null);
        if (state == null) {
            state = new OrderProcessState();
            state.setOrderId(orderId);
            state.setProcessInstanceId(execution.getProcessInstanceId());
        }
        state.setReserved(true);
        // переход к ожиданию решения по отгрузке (этап 4)
        state.setCurrentStage(OrderProcessStage.WAIT_CONFIRMATION);
        orderProcessRepository.save(state);

        execution.setVariable(ProcessVariables.RESERVED, true);
        execution.setVariable(ProcessVariables.CURRENT_STAGE, OrderProcessStage.WAIT_CONFIRMATION.name());
        log.info("Резервирование успешно завершено для заказа {}", orderId);
    }
}
