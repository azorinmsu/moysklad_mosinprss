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
import ru.moysklad.remap_1_2.responses.ListEntity;
import ru.moysklad.remap_1_2.utils.ApiClientException;
import ru.moysklad.remap_1_2.utils.params.ExpandParam;
import ru.moysklad.remap_1_2.utils.params.FilterParam;

import java.util.ArrayList;
import java.util.UUID;

/**
* ШАГ 5
* */
@Component
public class CreateDemandStep {

    private static final Logger log = LoggerFactory.getLogger(CreateDemandStep.class);

    private final ApiClient apiClient;
    private final OrderProcessRepository orderProcessRepository;

    public CreateDemandStep(ApiClient apiClient, OrderProcessRepository orderProcessRepository) {
        this.apiClient = apiClient;
        this.orderProcessRepository = orderProcessRepository;
    }

    public void createDemand(DelegateExecution execution) throws Exception {
        UUID orderId = OrderStepUtils.extractOrderId(execution);
        // этап 5
        log.info("Этап 5: Создание отгрузки для заказа {}", orderId);

        OrderProcessState state = orderProcessRepository.findById(orderId).orElse(null);
        if (state == null) {
            state = new OrderProcessState();
            state.setOrderId(orderId);
            state.setProcessInstanceId(execution.getProcessInstanceId());
        }

        // 1. Проверяем, не зафиксирован ли demandId уже в БД или переменных процесса
        if (state.getDemandId() != null) {
            try {
                Demand existing = apiClient.entity().demand().get(state.getDemandId().toString());
                if (existing != null) {
                    log.info("Используем уже существующую отгрузку из состояния БД: {}", state.getDemandId());
                    execution.setVariable(ProcessVariables.DEMAND_ID, state.getDemandId().toString());
                    execution.setVariable(ProcessVariables.CURRENT_STAGE, OrderProcessStage.CREATE_DEMAND.name());
                    return;
                }
            } catch (ApiClientException e) {
                if (e.getStatusCode() != 404) {
                    log.error("Ошибка при проверке существующей отгрузки {}: HTTP {}", state.getDemandId(), e.getStatusCode(), e);
                    throw e;
                }
            }
        }

        // 2. Загружаем заказ с позициями
        CustomerOrder order;
        try {
            order = apiClient.entity().customerorder().get(orderId.toString(), ExpandParam.expand("positions"));
        } catch (ApiClientException e) {
            if (e.getStatusCode() == 404 || e.getStatusCode() == 400) {
                throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Заказ не найден в МойСклад: " + orderId);
            }
            throw e;
        }

        if (order == null) {
            throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Заказ не найден в МойСклад: " + orderId);
        }

        // 3. Дедупликация (для сценариев LOSE_RESPONSE и предварительно созданных отгрузок):
        // Проверяем, нет ли уже созданной отгрузки для этого заказа в МойСклад
        try {
            ListEntity<Demand> existingDemands = apiClient.entity().demand()
                    .get(FilterParam.filterEq("customerOrder", order.getMeta().getHref()));
            if (existingDemands != null && existingDemands.getRows() != null && !existingDemands.getRows().isEmpty()) {
                Demand found = existingDemands.getRows().get(0);
                UUID foundDemandId = UUID.fromString(found.getId());
                log.info("Найдена существующая отгрузка {} для заказа {}", foundDemandId, orderId);

                state.setDemandId(foundDemandId);
                state.setCurrentStage(OrderProcessStage.CREATE_DEMAND);
                orderProcessRepository.save(state);

                execution.setVariable(ProcessVariables.DEMAND_ID, foundDemandId.toString());
                execution.setVariable(ProcessVariables.CURRENT_STAGE, OrderProcessStage.CREATE_DEMAND.name());
                return;
            }
        } catch (Exception e) {
            log.warn("Не удалось выполнить поиск существующих отгрузок по фильтру: {}", e.getMessage());
        }

        // 4. Формируем черновик отгрузки (applicable = false)
        Demand demand = new Demand();
        demand.setCustomerOrder(order);
        demand.setApplicable(false);
        demand.setDescription("Отгрузка по заказу " + orderId);

        ListEntity<DemandDocumentPosition> demandPositions = new ListEntity<>();
        demandPositions.setRows(new ArrayList<>());
        if (order.getPositions() != null && order.getPositions().getRows() != null) {
            for (CustomerOrderDocumentPosition orderPos : order.getPositions().getRows()) {
                DemandDocumentPosition pos = new DemandDocumentPosition();
                pos.setAssortment(orderPos.getAssortment());
                pos.setQuantity(orderPos.getQuantity());
                pos.setPrice(orderPos.getPrice());
                demandPositions.getRows().add(pos);
            }
        }
        demand.setPositions(demandPositions);

        // 5. Создаем отгрузку через SDK
        Demand created;
        try {
            created = apiClient.entity().demand().create(demand);
        } catch (ApiClientException e) {
            if (e.getStatusCode() == 404 || e.getStatusCode() == 400) {
                log.warn("Бизнес-ошибка при создании отгрузки для заказа {}: HTTP {}", orderId, e.getStatusCode());
                throw new BpmnError(ProcessErrors.BUSINESS_ERROR, "Ошибка создания отгрузки: " + e.getMessage());
            }
            log.error("Временный технический сбой при создании отгрузки для заказа {}: HTTP {}", orderId, e.getStatusCode(), e);
            throw e;
        }

        UUID demandId = UUID.fromString(created.getId());
        state.setDemandId(demandId);
        state.setCurrentStage(OrderProcessStage.CREATE_DEMAND);
        orderProcessRepository.save(state);

        execution.setVariable(ProcessVariables.DEMAND_ID, demandId.toString());
        execution.setVariable(ProcessVariables.CURRENT_STAGE, OrderProcessStage.CREATE_DEMAND.name());
        log.info("Отгрузка {} успешно создана для заказа {}", demandId, orderId);
    }
}
