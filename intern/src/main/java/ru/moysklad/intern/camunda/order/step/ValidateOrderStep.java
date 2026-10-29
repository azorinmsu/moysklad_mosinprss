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
import ru.moysklad.intern.entity.order.OrderProcessStatus;
import ru.moysklad.intern.repo.OrderProcessRepository;
import ru.moysklad.remap_1_2.ApiClient;
import ru.moysklad.remap_1_2.entities.documents.CustomerOrder;
import ru.moysklad.remap_1_2.entities.documents.positions.CustomerOrderDocumentPosition;
import ru.moysklad.remap_1_2.utils.ApiClientException;
import ru.moysklad.remap_1_2.utils.params.ExpandParam;

import java.util.UUID;

@Component
public class ValidateOrderStep {

    private static final Logger log = LoggerFactory.getLogger(ValidateOrderStep.class);

    private final ApiClient apiClient;
    private final OrderProcessRepository orderProcessRepository;

    public ValidateOrderStep(ApiClient apiClient, OrderProcessRepository orderProcessRepository) {
        this.apiClient = apiClient;
        this.orderProcessRepository = orderProcessRepository;
    }

    public void validate(DelegateExecution execution) throws Exception {
        // извлекаем orderId из переменных процесса или businessKey
        UUID orderId = OrderStepUtils.extractOrderId(execution);

        log.info("Этап 1: Валидация заказа {}", orderId);

        // проверяем запрет на повторный запуск активного процесса для того же заказа
        OrderProcessState state = orderProcessRepository.findById(orderId).orElse(null);
        if (state != null && state.getStatus() == OrderProcessStatus.PROCESSING
                && !execution.getProcessInstanceId().equals(state.getProcessInstanceId())) {
            throw new BpmnError(ProcessErrors.BUSINESS_ERROR,
                    "Для заказа " + orderId + " уже запущен активный процесс: " + state.getProcessInstanceId());
        }

        // инициализируем/обновляем запись состояния в БД
        if (state == null) {
            state = new OrderProcessState();
            state.setOrderId(orderId);
        }
        state.setProcessInstanceId(execution.getProcessInstanceId());
        state.setStatus(OrderProcessStatus.PROCESSING);
        state.setCurrentStage(OrderProcessStage.VALIDATE_ORDER);
        orderProcessRepository.save(state);

        // загружаем заказ с позициями через SDK
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
            // 429 / 5xx / таймауты — временные технические сбои, пробрасываем дальше для Camunda retry
            log.error("Временный технический сбой при обращении к Remap API для заказа {}: HTTP {}", orderId, e.getStatusCode(), e);
            throw e;
        }

        if (order == null) {
            throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Заказ не найден в МойСклад: " + orderId);
        }

        // проверяем наличие позиций
        if (order.getPositions() == null || order.getPositions().getRows() == null || order.getPositions().getRows().isEmpty()) {
            throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Заказ " + orderId + " не содержит позиций");
        }

        // проверяем валидность позиций и количеств
        for (CustomerOrderDocumentPosition pos : order.getPositions().getRows()) {
            if (pos.getAssortment() == null) {
                throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "В позиции " + pos.getId() + " не указан товар");
            }
            if (pos.getQuantity() == null || pos.getQuantity() <= 0) {
                throw new BpmnError(ProcessErrors.BUSINESS_ERROR,
                        "В позиции " + pos.getId() + " некорректное количество: " + pos.getQuantity());
            }
        }

        // фиксируем переменные для дальнейших шагов
        execution.setVariable(ProcessVariables.ORDER_ID, orderId.toString());
        execution.setVariable(ProcessVariables.CURRENT_STAGE, OrderProcessStage.VALIDATE_ORDER.name());

        log.info("Этап 1 успешно завершён для заказа {}, позиций: {}", orderId, order.getPositions().getRows().size());
    }
}
