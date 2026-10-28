package ru.moysklad.intern.service.order;

import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.moysklad.intern.camunda.OrderProcessDefinition;
import ru.moysklad.intern.camunda.ProcessVariables;
import ru.moysklad.intern.dto.order.OrderProcessResponse;
import ru.moysklad.intern.entity.order.OrderProcessStage;
import ru.moysklad.intern.entity.order.OrderProcessState;
import ru.moysklad.intern.entity.order.OrderProcessStatus;
import ru.moysklad.intern.repo.OrderProcessRepository;

import java.util.Map;
import java.util.UUID;

@Service
public class OrderProcessService {

    private static final Logger log = LoggerFactory.getLogger(OrderProcessService.class);

    private final RuntimeService runtimeService;
    private final OrderProcessRepository orderProcessRepository;

    public OrderProcessService(RuntimeService runtimeService, OrderProcessRepository orderProcessRepository) {
        this.runtimeService = runtimeService;
        this.orderProcessRepository = orderProcessRepository;
    }

    @Transactional
    public OrderProcessResponse startProcess(UUID orderId) {
        log.info("Запуск обработки заказа {}", orderId);

        if (orderProcessRepository.hasActiveProcess(orderId)) {
            OrderProcessState active = orderProcessRepository.findById(orderId).orElseThrow();
            log.warn("Для заказа {} уже выполняется процесс {}", orderId, active.getProcessInstanceId());
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Для заказа " + orderId + " уже запущен активный процесс: " + active.getProcessInstanceId());
        }

        OrderProcessState state = orderProcessRepository.findById(orderId).orElseGet(() -> {
            OrderProcessState s = new OrderProcessState();
            s.setOrderId(orderId);
            return s;
        });

        state.setStatus(OrderProcessStatus.PROCESSING);
        state.setCurrentStage(OrderProcessStage.VALIDATE_ORDER);
        state.setDemandId(null);
        state.setReserved(false);
        state.setIrreversible(false);
        state.setErrorReason(null);

        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                OrderProcessDefinition.PROCESS_KEY,
                orderId.toString(),
                Map.of(ProcessVariables.ORDER_ID, orderId.toString())
        );

        state.setProcessInstanceId(instance.getId());
        state = orderProcessRepository.save(state);
        log.info("Процесс {} запущен для заказа {}", instance.getId(), orderId);

        return OrderProcessResponse.from(state);
    }

    @Transactional(readOnly = true)
    public OrderProcessResponse getStatus(UUID orderId) {
        OrderProcessState state = orderProcessRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Состояние заказа не найдено: " + orderId));
        return OrderProcessResponse.from(state);
    }

    @Transactional
    public OrderProcessResponse confirmDispatch(UUID orderId) {
        log.info("Подтверждение отгрузки для заказа {}", orderId);
        OrderProcessState state = orderProcessRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Состояние заказа не найдено: " + orderId));

        if (state.getStatus() != OrderProcessStatus.PROCESSING) {
            log.info("Заказ {} уже завершен со статусом {}, подтверждение пропущено", orderId, state.getStatus());
            return OrderProcessResponse.from(state);
        }

        try {
            runtimeService.createMessageCorrelation(OrderProcessDefinition.MSG_CONFIRM_DISPATCH)
                    .processInstanceBusinessKey(orderId.toString())
                    .correlate();
            log.info("Сообщение MSG_CONFIRM_DISPATCH коррелировано для заказа {}", orderId);
        } catch (MismatchingMessageCorrelationException e) {
            log.warn("Не найдено активное ожидание подтверждения для заказа {}: {}", orderId, e.getMessage());
        }

        OrderProcessState updated = orderProcessRepository.findById(orderId).orElse(state);
        return OrderProcessResponse.from(updated);
    }

    @Transactional
    public OrderProcessResponse rejectDispatch(UUID orderId) {
        log.info("Отклонение отгрузки для заказа {}", orderId);
        OrderProcessState state = orderProcessRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Состояние заказа не найдено: " + orderId));

        if (state.getStatus() != OrderProcessStatus.PROCESSING) {
            log.info("Заказ {} уже завершен со статусом {}, отклонение пропущено", orderId, state.getStatus());
            return OrderProcessResponse.from(state);
        }

        try {
            runtimeService.createMessageCorrelation(OrderProcessDefinition.MSG_REJECT_DISPATCH)
                    .processInstanceBusinessKey(orderId.toString())
                    .correlate();
            log.info("Сообщение MSG_REJECT_DISPATCH коррелировано для заказа {}", orderId);
        } catch (MismatchingMessageCorrelationException e) {
            log.warn("Не найдено активное ожидание отклонения для заказа {}: {}", orderId, e.getMessage());
        }

        OrderProcessState updated = orderProcessRepository.findById(orderId).orElse(state);
        return OrderProcessResponse.from(updated);
    }

    @Transactional
    public OrderProcessResponse cancelProcess(UUID orderId) {
        log.info("Запрос на отмену процесса для заказа {}", orderId);
        OrderProcessState state = orderProcessRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Состояние заказа не найдено: " + orderId));

        if (state.getStatus() == OrderProcessStatus.CANCELLED) {
            log.info("Заказ {} уже отменен", orderId);
            return OrderProcessResponse.from(state);
        }

        try {
            runtimeService.createMessageCorrelation(OrderProcessDefinition.MSG_CANCEL_ORDER)
                    .processInstanceBusinessKey(orderId.toString())
                    .correlate();
            log.info("Сообщение MSG_CANCEL_ORDER коррелировано для заказа {}", orderId);
        } catch (MismatchingMessageCorrelationException e) {
            log.warn("Не найдено подходящее ожидание отмены для заказа {}: {}", orderId, e.getMessage());
        }

        OrderProcessState updated = orderProcessRepository.findById(orderId).orElse(state);
        return OrderProcessResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public OrderProcessResponse getResult(UUID orderId) {
        OrderProcessState state = orderProcessRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Состояние заказа не найдено: " + orderId));
        return OrderProcessResponse.from(state);
    }
}
