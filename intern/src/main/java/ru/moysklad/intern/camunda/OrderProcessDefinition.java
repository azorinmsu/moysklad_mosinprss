package ru.moysklad.intern.camunda;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
* Процесс в виде бпмнки
* <img src="order/order.png">
 *    ух ты, джавадок даже так умеет
**/

@Component
public class OrderProcessDefinition {
    // имя процесса
    public static final String PROCESS_KEY = "order-processing";
    public static final String PROCESS_NAME = "Обработка заказа покупателя с резервированием и отгрузкой";
    public static final String STEPS = "ORDER_STEPS";
    // сами процессы
    public static final String START = "ORDER_START";
    public static final String SUB_START = "ORDER_SUBPROCESS_START";

    public static final String VALIDATE = "VALIDATE_ORDER"; // 1. получение и проверка заказа
    public static final String CHECK_STOCK = "CHECK_STOCK_ORDER"; // 2. проверка остатков
    public static final String RESERVE = "RESERVE_ORDER"; // 3. резервирование
    public static final String WAIT_CONFIRMATION = "WAIT_CONFIRMATION_ORDER"; // 4. ожидание подтверждения отгрузки
    public static final String CREATE_DEMAND = "CREATE_DEMAND_ORDER"; // 5. создание отгрузки (demand)
    public static final String VALIDATE_DEMAND = "VALIDATE_DEMAND_ORDER"; // 6.проверка отгрузки
    public static final String CONFIRM_DEMAND = "CONFIRM_DEMAND_ORDER";
    public static final String COMPLETE = "COMPLETE_ORDER";

    public static final String SUB_END = "ORDER_SUBPROCESS_END";
    public static final String END = "ORDER_END";

    // ошибки
    public static final String END_CANCELLED = "ORDER_CANCELLED_END";
    public static final String END_FAILED = "ORDER_FAILED_END";

    // компенсация
    public static final String BOUNDARY_COMPENSATE_RESERVE = "BOUNDARY_COMPENSATE_RESERVE";
    public static final String TASK_COMPENSATE_RESERVE = "TASK_COMPENSATE_RESERVE";

    public static final String BOUNDARY_COMPENSATE_DEMAND = "BOUNDARY_COMPENSATE_DEMAND";
    public static final String TASK_COMPENSATE_DEMAND = "TASK_COMPENSATE_DEMAND";

    // шлюзы и таймеры
    public static final String GATEWAY_STOCK_AVAILABLE = "GATEWAY_STOCK_AVAILABLE";
    public static final String TIMER_STOCK_RETRY = "TIMER_STOCK_RETRY";

    // события
    public static final String EVENT_MSG_CONFIRM = "MSG_CONFIRM_DISPATCH";
    public static final String EVENT_MSG_REJECT = "MSG_REJECT_DISPATCH";
    public static final String TIMER_DISPATCH_TIMEOUT = "TIMER_DISPATCH_TIMEOUT";

    // таймер
    private final String timerCycle;

    public OrderProcessDefinition(@Value("${app.camunda.sales-timer-cycle:R/PT30S}") String timerCycle) {
        this.timerCycle = timerCycle;
    }
    public BpmnModelInstance model() {
        return Bpmn.createExecutableProcess(PROCESS_KEY)
                .name(PROCESS_NAME)
                .startEvent(START)
                .subProcess(STEPS)
                .embeddedSubProcess()
                    .startEvent(SUB_START)
                // 1
                        .serviceTask(VALIDATE)
                            .name("Validate buyer order")
                            .camundaDelegateExpression(delegate("")) // пока заглушки
                // 2
                        .serviceTask(CHECK_STOCK)
                            .name("Check stock availability")
                            .camundaDelegateExpression(delegate(""))
                // проверка
                        .exclusiveGateway(GATEWAY_STOCK_AVAILABLE) // проверка на достаточность остатков
                            .name("Is stock sufficient?")
                            .condition("Sufficient", "${stockSufficient == true}")
                // 3
                        .serviceTask(RESERVE)
                            .name("Reserve stock")
                            .camundaDelegateExpression(delegate(""))
                // компенсация
                        .boundaryEvent(BOUNDARY_COMPENSATE_RESERVE)
                            .compensationStart()
                                .serviceTask(TASK_COMPENSATE_RESERVE) // граничное событие компенсации для снятия резерва
                                    .name("Compensate Release Reservation")
                                    .camundaDelegateExpression(delegate(""))
                            .compensationDone()
                        .moveToActivity(RESERVE)
                // 4 - ожидание подтверждения отгрузки
                        .intermediateCatchEvent(WAIT_CONFIRMATION)
                            .name("Wait for dispatch approval") // ждем...
                            .message("MSG_DISPATCH_APPROVED")
                // 5
                        .serviceTask(CREATE_DEMAND)
                            .name("Create demand") // проверка созданной отгрузки
                            .camundaDelegateExpression(delegate(""))
                // Компенсация для отмены/удаления черновика отгрузки
                        .boundaryEvent(BOUNDARY_COMPENSATE_DEMAND)
                            .compensationStart()
                                .serviceTask(TASK_COMPENSATE_DEMAND)
                                    .name("Compensate: Cancel/Delete Demand Draft")
                                    .camundaDelegateExpression(delegate(""))
                            .compensationDone()
                        .moveToActivity(CREATE_DEMAND)
                // 6
                        .serviceTask(VALIDATE_DEMAND)
                            .name("Validate demand")
                            .camundaDelegateExpression(delegate(""))
                // 7
                        .serviceTask(CONFIRM_DEMAND)
                            .name("Confirm demand")
                            .camundaDelegateExpression(delegate(""))
                // 8
                        .serviceTask(COMPLETE)
                            .name("Complete order processing")
                            .camundaDelegateExpression(delegate(""))
                    .endEvent(SUB_END)
                // ветка недостатка остатков
                .moveToNode(GATEWAY_STOCK_AVAILABLE)
                    .condition("Insufficient", "${stockSufficient == false}")
                    .intermediateCatchEvent(TIMER_STOCK_RETRY)
                        .name("wait for stock replenishment")
                        .timerWithDuration(timerCycle)
                    .connectTo(CHECK_STOCK)
                .subProcessDone()

                .endEvent(END)
                    .name("Order Done") // конец заказа

                // событие отмены
                .moveToActivity(STEPS)
                .boundaryEvent("ORDER_CANCEL_EVENT")
                    .message("MSG_CANCEL_ORDER")
                .intermediateThrowEvent("THROW_COMPENSATION")
                    .compensateEventDefinition()
                    .compensateEventDefinitionDone()
                .serviceTask("HANDLE_CANCELLATION")
                    .name("Record Cancelled State")
                    .camundaDelegateExpression(delegate("handleCancellationDelegate"))
                .endEvent(END_CANCELLED)
                    .name("Order Cancelled")
                .done();
    }

    private static String delegate(String beanName) {
        return "${" + beanName + "}";
    }
}
