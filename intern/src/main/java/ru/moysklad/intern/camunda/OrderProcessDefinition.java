package ru.moysklad.intern.camunda;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.order.delegate.*;

/**
 * весь процесс обработки заказа
 */
@Component
public class OrderProcessDefinition {

    public static final String PROCESS_KEY = "order-processing";
    public static final String PROCESS_NAME = "Обработка заказа покупателя с резервированием и отгрузкой";

    private static final int HISTORY_TTL_DAYS = 30;
    private static final String RETRY_CYCLE = "R3/PT10S";

    public static final String START = "ORDER_START";

    // первый и второй этап
    public static final String VALIDATE_ORDER = "VALIDATE_ORDER";
    public static final String CHECK_STOCK = "CHECK_STOCK_ORDER";
    public static final String GATEWAY_STOCK = "GATEWAY_STOCK_AVAILABLE";
    public static final String TIMER_STOCK_RETRY = "TIMER_STOCK_RETRY";
    public static final String RECORD_STOCK_TIMEOUT = "RECORD_STOCK_TIMEOUT";
    public static final String END_STOCK_TIMEOUT = "END_STOCK_TIMEOUT";

    // третий этап
    public static final String RESERVE_ORDER = "RESERVE_ORDER";
    public static final String BOUNDARY_COMPENSATE_RESERVE = "BOUNDARY_COMPENSATE_RESERVE";
    public static final String TASK_COMPENSATE_RESERVE = "TASK_COMPENSATE_RESERVE";

    // четвертый этап
    public static final String GATEWAY_DISPATCH = "GATEWAY_DISPATCH_DECISION";
    public static final String EVENT_DISPATCH_APPROVED = "EVENT_DISPATCH_APPROVED";
    public static final String EVENT_DISPATCH_REJECTED = "EVENT_DISPATCH_REJECTED";
    public static final String EVENT_DISPATCH_TIMEOUT = "EVENT_DISPATCH_TIMEOUT";
    public static final String THROW_COMPENSATE_REJECTED = "THROW_COMPENSATE_REJECTED";
    public static final String RECORD_REJECTED = "RECORD_REJECTED";
    public static final String END_REJECTED = "END_REJECTED";
    public static final String THROW_COMPENSATE_DISPATCH_TIMEOUT = "THROW_COMPENSATE_DISPATCH_TIMEOUT";
    public static final String RECORD_DISPATCH_TIMEOUT = "RECORD_DISPATCH_TIMEOUT";
    public static final String END_DISPATCH_TIMEOUT = "END_DISPATCH_TIMEOUT";

    // оставшиеся этапы
    public static final String CREATE_DEMAND = "CREATE_DEMAND_ORDER";
    public static final String BOUNDARY_COMPENSATE_DEMAND = "BOUNDARY_COMPENSATE_DEMAND";
    public static final String TASK_COMPENSATE_DEMAND = "TASK_COMPENSATE_DEMAND";
    public static final String VALIDATE_DEMAND = "VALIDATE_DEMAND_ORDER";
    public static final String CONFIRM_DEMAND = "CONFIRM_DEMAND_ORDER";
    public static final String COMPLETE_ORDER = "COMPLETE_ORDER";
    public static final String END_COMPLETED = "END_ORDER_COMPLETED";

    // Отмена
    public static final String EVENT_SUB_CANCEL = "EVENT_SUB_CANCEL_ORDER";
    public static final String CANCEL_START = "CANCEL_START";
    public static final String GATEWAY_CANCEL = "GATEWAY_CANCEL_ALLOWED";
    public static final String CANCEL_COMP_SUB = "CANCEL_COMPENSATION";
    public static final String CANCEL_COMP_START = "CANCEL_COMPENSATION_START";
    public static final String THROW_COMPENSATE_CANCEL = "THROW_COMPENSATE_CANCEL";
    public static final String RECORD_CANCELLED = "RECORD_CANCELLED";
    public static final String CANCEL_COMP_END = "CANCEL_COMPENSATION_END";
    public static final String BOUNDARY_CANCEL_COMP_ERROR = "BOUNDARY_CANCEL_COMP_ERROR";
    public static final String RECORD_COMPENSATION_FAILED = "RECORD_COMPENSATION_FAILED";
    public static final String END_COMPENSATION_FAILED = "END_COMPENSATION_FAILED";
    public static final String RECORD_CANCEL_REFUSED = "RECORD_CANCEL_REFUSED";
    public static final String END_CANCEL_REFUSED = "END_CANCEL_REFUSED";
    public static final String END_CANCELLED = "END_ORDER_CANCELLED";

    // бизнес-ошибки
    public static final String EVENT_SUB_ERROR = "EVENT_SUB_ORDER_ERROR";
    public static final String ERROR_START = "ERROR_START";
    public static final String GATEWAY_ERROR = "GATEWAY_ERROR_REVERSIBLE";
    public static final String ERROR_COMP_SUB = "ERROR_COMPENSATION";
    public static final String ERROR_COMP_START = "ERROR_COMPENSATION_START";
    public static final String THROW_COMPENSATE_ERROR = "THROW_COMPENSATE_ERROR";
    public static final String RECORD_FAILED = "RECORD_FAILED";
    public static final String ERROR_COMP_END = "ERROR_COMPENSATION_END";
    public static final String BOUNDARY_ERROR_COMP_ERROR = "BOUNDARY_ERROR_COMP_ERROR";
    public static final String RECORD_ERROR_COMPENSATION_FAILED = "RECORD_ERROR_COMPENSATION_FAILED";
    public static final String END_ERROR_COMPENSATION_FAILED = "END_ERROR_COMPENSATION_FAILED";
    public static final String RECORD_FAILED_AFTER_POSTING = "RECORD_FAILED_AFTER_POSTING";
    public static final String END_FAILED_AFTER_POSTING = "END_FAILED_AFTER_POSTING";
    public static final String END_FAILED = "END_ORDER_FAILED";

    // сообщения об ошибках
    public static final String MSG_CONFIRM_DISPATCH = "MSG_CONFIRM_DISPATCH";
    public static final String MSG_REJECT_DISPATCH = "MSG_REJECT_DISPATCH";
    public static final String MSG_CANCEL_ORDER = "MSG_CANCEL_ORDER";

    private final String stockRetryDuration;
    private final String dispatchTimeoutDuration;

    public OrderProcessDefinition(
            @Value("${app.camunda.order-stock-retry-duration:PT30S}") String stockRetryDuration,
            @Value("${app.camunda.order-dispatch-timeout:PT24H}") String dispatchTimeoutDuration
    ) {
        this.stockRetryDuration = stockRetryDuration;
        this.dispatchTimeoutDuration = dispatchTimeoutDuration;
    }

    public BpmnModelInstance model() {
        ProcessBuilder process = Bpmn.createExecutableProcess(PROCESS_KEY)
                .name(PROCESS_NAME)
                .camundaHistoryTimeToLive(HISTORY_TTL_DAYS);

        process.startEvent(START)
                .name("Запуск обработки заказа")

                // 1 получение и проверка заказа
                .serviceTask(VALIDATE_ORDER)
                    .name("получение и проверка заказа")
                    .camundaAsyncBefore()
                    .camundaFailedJobRetryTimeCycle(RETRY_CYCLE)
                    .camundaDelegateExpression(bean(ValidateOrderDelegate.BEAN_NAME))

                // 2 проверка остатков
                .serviceTask(CHECK_STOCK)
                    .name("проверка остатков")
                    .camundaAsyncBefore()
                    .camundaFailedJobRetryTimeCycle(RETRY_CYCLE)
                    .camundaDelegateExpression(bean(CheckStockDelegate.BEAN_NAME))

                .exclusiveGateway(GATEWAY_STOCK)
                    .name("остатков достаточно?")
                    .condition("Достаточно", "${stockSufficient == true}")

                // 3 резервирование
                .serviceTask(RESERVE_ORDER)
                    .name("резервирование")
                    .camundaAsyncBefore()
                    .camundaFailedJobRetryTimeCycle(RETRY_CYCLE)
                    .camundaDelegateExpression(bean(ReserveStockDelegate.BEAN_NAME))

                .boundaryEvent(BOUNDARY_COMPENSATE_RESERVE)
                    .compensateEventDefinition()
                    .compensateEventDefinitionDone()

                .compensationStart()
                    .serviceTask(TASK_COMPENSATE_RESERVE)
                        .name("снять резерв (комп)") // компенсация
                        .camundaDelegateExpression(bean(ReleaseReservationDelegate.BEAN_NAME))
                .compensationDone()
                .moveToActivity(RESERVE_ORDER)

                // 4 ожидание подтверждения
                .eventBasedGateway()
                .id(GATEWAY_DISPATCH)
                    .name("ожидание решения по отгрузке")

                .intermediateCatchEvent(EVENT_DISPATCH_APPROVED)
                    .name("Отгрузка подтверждена")
                    .message(MSG_CONFIRM_DISPATCH)

                // 5 создание отгрузки
                .serviceTask(CREATE_DEMAND)
                    .name("создание отгрузки")
                    .camundaAsyncBefore()
                    .camundaFailedJobRetryTimeCycle(RETRY_CYCLE)
                    .camundaDelegateExpression(bean(CreateDemandDelegate.BEAN_NAME))

                .boundaryEvent(BOUNDARY_COMPENSATE_DEMAND)
                    .compensateEventDefinition()
                    .compensateEventDefinitionDone()

                .compensationStart()
                    .serviceTask(TASK_COMPENSATE_DEMAND)
                        .name("удалить черновик отгрузки (комп)")
                        .camundaDelegateExpression(bean(CancelDemandDraftDelegate.BEAN_NAME))
                .compensationDone()
                .moveToActivity(CREATE_DEMAND)

                // 6 проверка отгрузки
                .serviceTask(VALIDATE_DEMAND)
                    .name("проверка отгрузки")
                    .camundaAsyncBefore()
                    .camundaFailedJobRetryTimeCycle(RETRY_CYCLE)
                    .camundaDelegateExpression(bean(ValidateDemandDelegate.BEAN_NAME))

                // 7 проведение отгрузки — точка невозврата
                .serviceTask(CONFIRM_DEMAND)
                    .name("подтверждение отгрузки")
                    .camundaAsyncBefore()
                    .camundaFailedJobRetryTimeCycle(RETRY_CYCLE)
                    .camundaDelegateExpression(bean(ConfirmDemandDelegate.BEAN_NAME))

                // 8 завершение
                .serviceTask(COMPLETE_ORDER)
                    .name("завершение обработки")
                    .camundaAsyncBefore()
                    .camundaFailedJobRetryTimeCycle(RETRY_CYCLE)
                    .camundaDelegateExpression(bean(CompleteOrderDelegate.BEAN_NAME))
                .endEvent(END_COMPLETED)
                    .name("заказ выполнен")

                // ветка отклонения отгрузки
                .moveToNode(GATEWAY_DISPATCH)
                .intermediateCatchEvent(EVENT_DISPATCH_REJECTED)
                    .name("отгрузка отклонена")
                    .message(MSG_REJECT_DISPATCH)

                .intermediateThrowEvent(THROW_COMPENSATE_REJECTED)
                    .compensateEventDefinition()
                    .compensateEventDefinitionDone()
                .serviceTask(RECORD_REJECTED)
                    .name("зафиксировать отклонение")
                    .camundaDelegateExpression(bean(RecordOrderOutcomeDelegate.BEAN_NAME))
                    .camundaInputParameter("outcome", "REJECTED")
                .endEvent(END_REJECTED)
                    .name("заказ отклонён")

                // ветка таймаута ожидания подтверждения
                .moveToNode(GATEWAY_DISPATCH)
                .intermediateCatchEvent(EVENT_DISPATCH_TIMEOUT)
                    .name("таймаут ожидания подтверждения")
                    .timerWithDuration(dispatchTimeoutDuration)

                .intermediateThrowEvent(THROW_COMPENSATE_DISPATCH_TIMEOUT)
                    .compensateEventDefinition()
                    .compensateEventDefinitionDone()

                .serviceTask(RECORD_DISPATCH_TIMEOUT)
                    .name("зафиксировать таймаут")
                    .camundaDelegateExpression(bean(RecordOrderOutcomeDelegate.BEAN_NAME))
                    .camundaInputParameter("outcome", "TIMEOUT")
                .endEvent(END_DISPATCH_TIMEOUT)
                    .name("таймаут подтверждения")

                // ветка нехватки остатков - ожидание и повтор проверки
                .moveToNode(GATEWAY_STOCK)
                .condition(
                        "не хватает, лимит ожидания не исчерпан",
                        "${stockSufficient == false && stockWaitExpired == false}"
                )

                .intermediateCatchEvent(TIMER_STOCK_RETRY)
                    .name("Ожидание пополнения остатков")
                    .timerWithDuration(stockRetryDuration)
                    .connectTo(CHECK_STOCK)

                // Ветка исчерпания лимита ожидания
                .moveToNode(GATEWAY_STOCK)
                .condition(
                        "лимит ожидания остатков исчерпан",
                        "${stockSufficient == false && stockWaitExpired == true}"
                )
                .serviceTask(RECORD_STOCK_TIMEOUT)
                    .name("зафиксировать нехватку остатков")
                    .camundaDelegateExpression(bean(RecordOrderOutcomeDelegate.BEAN_NAME))
                    .camundaInputParameter("outcome", "STOCK_TIMEOUT")
                .endEvent(END_STOCK_TIMEOUT)
                    .name("недостаточно остатков");

        // interrupting event subprocess или остановить все
        process.eventSubProcess(EVENT_SUB_CANCEL)
                .startEvent(CANCEL_START)
                    .name("запрос отмены")
                    .message(MSG_CANCEL_ORDER)

                .exclusiveGateway(GATEWAY_CANCEL)
                    .name("отмена ещё допустима?")
                    .condition("ддопустима", "${irreversible != true}") // последний шанс

                .subProcess(CANCEL_COMP_SUB)
                    .embeddedSubProcess()
                    .startEvent(CANCEL_COMP_START)

                    .intermediateThrowEvent(THROW_COMPENSATE_CANCEL)
                        .compensateEventDefinition()
                        .compensateEventDefinitionDone()
                    .serviceTask(RECORD_CANCELLED)
                        .name("зафиксировать отмену")
                        .camundaDelegateExpression(bean(HandleCancellationDelegate.BEAN_NAME))
                    .endEvent(CANCEL_COMP_END)
                .subProcessDone()

                .boundaryEvent(BOUNDARY_CANCEL_COMP_ERROR) // не получилось
                    .error(ProcessErrors.BUSINESS_ERROR)
                    .serviceTask(RECORD_COMPENSATION_FAILED)
                        .name("зафиксировать ошибку компенсации")
                            .camundaDelegateExpression(bean(RecordOrderOutcomeDelegate.BEAN_NAME))
                        .camundaInputParameter("outcome", "COMPENSATION_FAILED")
                    .endEvent(END_COMPENSATION_FAILED)
                        .name("компенсация не удалась")
                .moveToActivity(CANCEL_COMP_SUB)
                .endEvent(END_CANCELLED)
                        .name("заказ отменён")

                .moveToNode(GATEWAY_CANCEL)
                .condition("уже необратимо", "${irreversible == true}")
                .serviceTask(RECORD_CANCEL_REFUSED)
                    .name("зафиксировать отказ от отмены")
                        .camundaDelegateExpression(bean(RecordOrderOutcomeDelegate.BEAN_NAME))
                        .camundaInputParameter("outcome", "CANCEL_REFUSED")
                .endEvent(END_CANCEL_REFUSED)
                    .name("отмена отклонена");

        // бизнес-ошибка любого этапа (с ее компенсацией, если это возможно)
        process.eventSubProcess(EVENT_SUB_ERROR)
                .startEvent(ERROR_START)
                    .name("бизнес-ошибка")
                    .error(ProcessErrors.BUSINESS_ERROR)

                .exclusiveGateway(GATEWAY_ERROR)
                    .name("компенсация ещё допустима?")
                    .condition("допустима", "${irreversible != true}")

                .subProcess(ERROR_COMP_SUB)
                    .embeddedSubProcess()
                    .startEvent(ERROR_COMP_START)

                    .intermediateThrowEvent(THROW_COMPENSATE_ERROR)
                        .compensateEventDefinition()
                        .compensateEventDefinitionDone()
                    .serviceTask(RECORD_FAILED)
                        .name("зафиксировать ошибку")
                        .camundaDelegateExpression(bean(RecordOrderOutcomeDelegate.BEAN_NAME))
                        .camundaInputParameter("outcome", "FAILED")
                    .endEvent(ERROR_COMP_END)
                .subProcessDone()
                // проблема с компенсацией
                .boundaryEvent(BOUNDARY_ERROR_COMP_ERROR)
                    .error(ProcessErrors.BUSINESS_ERROR)

                    .serviceTask(RECORD_ERROR_COMPENSATION_FAILED)
                        .name("зафиксировать ошибку компенсации")
                        .camundaDelegateExpression(bean(RecordOrderOutcomeDelegate.BEAN_NAME))
                        .camundaInputParameter("outcome", "COMPENSATION_FAILED")

                    .endEvent(END_ERROR_COMPENSATION_FAILED)
                        .name("компенсация не удалась")

                .moveToActivity(ERROR_COMP_SUB)
                .endEvent(END_FAILED)
                    .name("заказ не выполнен")
                .moveToNode(GATEWAY_ERROR)
                .condition("уже необратимо", "${irreversible == true}")
                .serviceTask(RECORD_FAILED_AFTER_POSTING)
                    .name("зафиксировать ошибку после проведения")
                    .camundaDelegateExpression(bean(RecordOrderOutcomeDelegate.BEAN_NAME))
                    .camundaInputParameter("outcome", "FAILED_AFTER_POSTING")
                .endEvent(END_FAILED_AFTER_POSTING)
                    .name("ошибка после проведения");

        return process.done();
    }

    private static String bean(String beanName) {
        return "${" + beanName + "}";
    }
}