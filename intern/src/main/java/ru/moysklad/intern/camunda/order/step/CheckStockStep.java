package ru.moysklad.intern.camunda.order.step;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ШАГ 2
 */
@Component
public class CheckStockStep {

    private static final Logger log = LoggerFactory.getLogger(CheckStockStep.class);

    private final ApiClient apiClient;
    private final RestClient remapRestClient;
    private final OrderProcessRepository orderProcessRepository;
    private final int maxRetries;

    public CheckStockStep(
            ApiClient apiClient,
            RestClient remapRestClient,
            OrderProcessRepository orderProcessRepository,
            @Value("${app.camunda.order-stock-max-retries:3}") int maxRetries
    ) {
        this.apiClient = apiClient;
        this.remapRestClient = remapRestClient;
        this.orderProcessRepository = orderProcessRepository;
        this.maxRetries = maxRetries;
    }

    public void checkStock(DelegateExecution execution) throws Exception {
        UUID orderId = OrderStepUtils.extractOrderId(execution);
        log.info("проверка остатков для заказа {}", orderId);

        // получаем позиции заказа через SDK
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
            log.error("Временный технический сбой при обращении к Remap API для заказа {}: HTTP {}", orderId, e.getStatusCode(), e);
            throw e;
        }

        if (order == null || order.getPositions() == null || order.getPositions().getRows() == null || order.getPositions().getRows().isEmpty()) {
            throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Заказ " + orderId + " не содержит позиций");
        }

        // получаем отчет по остаткам через RestClient
        Map<String, Double> availableStockByProduct = new HashMap<>();
        try {
            Map<String, Object> report = remapRestClient.get()
                    .uri("/report/stock/all")
                    .retrieve()
                    .body(Map.class);

            if (report != null && report.get("rows") instanceof List<?> rows) {
                for (Object item : rows) {
                    if (item instanceof Map<?, ?> row) {
                        Map<?, ?> meta = (Map<?, ?>) row.get("meta");
                        if (meta != null && meta.get("href") != null) {
                            String href = String.valueOf(meta.get("href"));
                            String prodId = OrderStepUtils.extractIdFromHref(href);
                            Object qtyObj = row.get("quantity");
                            double available = qtyObj instanceof Number num ? num.doubleValue() : 0.0;
                            availableStockByProduct.put(prodId, available);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Временная ошибка при запросе остатков /report/stock/all для заказа {}: {}", orderId, e.getMessage(), e);
            throw e;
        }

        // агрегируем требуемое количество по каждому товару
        Map<String, Double> requiredStockByProduct = new HashMap<>();
        for (CustomerOrderDocumentPosition pos : order.getPositions().getRows()) {
            String prodId = OrderStepUtils.extractProductId(pos.getAssortment());
            if (prodId == null) {
                throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Не удалось определить товар для позиции " + pos.getId());
            }
            double qty = pos.getQuantity() != null ? pos.getQuantity() : 0.0;
            requiredStockByProduct.merge(prodId, qty, Double::sum);
        }

        // проверяем достаточность остатков
        boolean stockSufficient = true;
        for (Map.Entry<String, Double> entry : requiredStockByProduct.entrySet()) {
            String prodId = entry.getKey();
            double required = entry.getValue();
            double available = availableStockByProduct.getOrDefault(prodId, 0.0);
            if (available < required) {
                stockSufficient = false;
                log.info("Недостаточно остатка для товара {}: требуется {}, доступно {}", prodId, required, available);
                break;
            }
        }

        // обновляем состояние процесса в БД
        OrderProcessState state = orderProcessRepository.findById(orderId).orElse(null);
        if (state == null) {
            state = new OrderProcessState();
            state.setOrderId(orderId);
            state.setProcessInstanceId(execution.getProcessInstanceId());
        }
        state.setCurrentStage(OrderProcessStage.CHECK_STOCK);

        if (stockSufficient) {
            log.info("Остатков достаточно для заказа {}", orderId);
            execution.setVariable(ProcessVariables.STOCK_SUFFICIENT, true);
            execution.setVariable(ProcessVariables.STOCK_WAIT_EXPIRED, false);
        } else {
            Integer retryCount = (Integer) execution.getVariable(ProcessVariables.STOCK_RETRY_COUNT);
            if (retryCount == null) {
                retryCount = 0;
            }
            retryCount++;
            execution.setVariable(ProcessVariables.STOCK_RETRY_COUNT, retryCount);
            execution.setVariable(ProcessVariables.STOCK_SUFFICIENT, false);

            boolean expired = retryCount >= maxRetries;
            execution.setVariable(ProcessVariables.STOCK_WAIT_EXPIRED, expired);
            log.info("Остатков недостаточно для заказа {}. Попытка {}/{}. Лимит ожидания исчерпан: {}",
                    orderId, retryCount, maxRetries, expired);
        }

        orderProcessRepository.save(state);
    }
}
