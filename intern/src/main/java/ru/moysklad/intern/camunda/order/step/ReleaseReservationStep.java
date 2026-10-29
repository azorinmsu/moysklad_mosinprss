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
 * если заказ отклонен оператором, случился таймаут или отмена
 * для шага 5
 */
@Component
public class ReleaseReservationStep {

    private static final Logger log = LoggerFactory.getLogger(ReleaseReservationStep.class);

    private final ApiClient apiClient;
    private final OrderProcessRepository orderProcessRepository;

    public ReleaseReservationStep(ApiClient apiClient, OrderProcessRepository orderProcessRepository) {
        this.apiClient = apiClient;
        this.orderProcessRepository = orderProcessRepository;
    }

    public void release(DelegateExecution execution) throws Exception {
        UUID orderId = OrderStepUtils.extractOrderId(execution);
        log.info("Компенсация: Снятие резерва для заказа {}", orderId);

        // загружаем заказ с позициями
        CustomerOrder order;
        try {
            order = apiClient.entity().customerorder().get(orderId.toString(), ExpandParam.expand("positions"));
        } catch (ApiClientException e) {
            if (e.getStatusCode() == 404) {
                log.warn("Заказ {} не найден при компенсации (HTTP 404)", orderId);
                return;
            }
            log.error("Сбой при получении заказа {} при компенсации: HTTP {}", orderId, e.getStatusCode(), e);
            throw e;
        }

        if (order != null && order.getPositions() != null && order.getPositions().getRows() != null) {
            // снимаем резерв: reserve = 0
            for (CustomerOrderDocumentPosition pos : order.getPositions().getRows()) {
                Double currentReserve = pos.getReserve();
                if (currentReserve != null && currentReserve > 0) {
                    try {
                        CustomerOrderDocumentPosition updatePos = new CustomerOrderDocumentPosition();
                        updatePos.setReserve(0.0);
                        apiClient.entity().customerorder().updatePosition(orderId.toString(), pos.getId(), updatePos);
                        log.info("Резерв позиции {} заказа {} обнулен", pos.getId(), orderId);
                    } catch (ApiClientException e) {
                        log.error("Сбой при обнулении резерва для позиции {} заказа {}: HTTP {}",
                                pos.getId(), orderId, e.getStatusCode(), e);
                        throw e;
                    }
                }
            }
        }

        // обновляем флаг reserved в БД
        OrderProcessState state = orderProcessRepository.findById(orderId).orElse(null);
        if (state != null) {
            state.setReserved(false);
            orderProcessRepository.save(state);
        }

        execution.setVariable(ProcessVariables.RESERVED, false);
        log.info("Снятие резерва успешно завершено для заказа {}", orderId);
    }
}
