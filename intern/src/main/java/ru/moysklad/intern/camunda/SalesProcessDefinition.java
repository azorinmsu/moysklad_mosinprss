package ru.moysklad.intern.camunda;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.delegate.CheckAvailableAmountDelegate;
import ru.moysklad.intern.camunda.delegate.CheckSalesStockDelegate;
import ru.moysklad.intern.camunda.delegate.ClaimPendingSalesDelegate;
import ru.moysklad.intern.camunda.delegate.PublishFailureDelegate;
import ru.moysklad.intern.camunda.delegate.PublishResultDelegate;
import ru.moysklad.intern.camunda.delegate.WriteOffStockDelegate;

/**
 * Процесс реализации и отдельный timer-процесс, который периодически забирает сохранённые PENDING-заявки.
 * Чтение Kafka timer не выполняет.
 */
@Component
public class SalesProcessDefinition {

    public static final String PROCESS_KEY = "sales";
    public static final String TIMER_PROCESS_KEY = "sales-pending-timer";

    public static final String START = "SALES_START";
    public static final String STEPS = "SALES_STEPS";
    public static final String CHECK_SALES_STOCK = "CHECK_SALES_STOCK";
    public static final String CHECK_AVAILABLE_AMOUNT = "CHECK_AVAILABLE_AMOUNT";
    public static final String WRITE_OFF_STOCK = "WRITE_OFF_STOCK";
    public static final String PUBLISH_RESULT = "SALES_PUBLISH_RESULT";
    public static final String END = "SALES_END";
    public static final String BUSINESS_ERROR_BOUNDARY = "SALES_BUSINESS_ERROR";
    public static final String PUBLISH_FAILURE = "SALES_PUBLISH_FAILURE";
    public static final String FAILURE_END = "SALES_FAILURE_END";

    public static final String TIMER_START = "SALES_TIMER_START";
    public static final String CLAIM_PENDING_SALES = "CLAIM_PENDING_SALES";
    public static final String TIMER_END = "SALES_TIMER_END";

    private final String timerCycle;

    public SalesProcessDefinition(@Value("${app.camunda.sales-timer-cycle:R/PT30S}") String timerCycle) {
        this.timerCycle = timerCycle;
    }

    public BpmnModelInstance model() {
        return Bpmn.createExecutableProcess(PROCESS_KEY)
                .name("Sales")
                .startEvent(START)
                .subProcess(STEPS)
                    .embeddedSubProcess()
                        .startEvent("SALES_STEPS_START")
                        .serviceTask(CHECK_SALES_STOCK)
                            .name("Check sales stock")
                            .camundaDelegateExpression(delegate(CheckSalesStockDelegate.BEAN_NAME))
                        .serviceTask(CHECK_AVAILABLE_AMOUNT)
                            .name("Check available amount")
                            .camundaDelegateExpression(delegate(CheckAvailableAmountDelegate.BEAN_NAME))
                        .serviceTask(WRITE_OFF_STOCK)
                            .name("Write off stock")
                            .camundaDelegateExpression(delegate(WriteOffStockDelegate.BEAN_NAME))
                        .endEvent("SALES_STEPS_END")
                    .subProcessDone()
                .serviceTask(PUBLISH_RESULT)
                    .name("Publish result")
                    .camundaDelegateExpression(delegate(PublishResultDelegate.BEAN_NAME))
                .endEvent(END)
                .moveToActivity(STEPS)
                .boundaryEvent(BUSINESS_ERROR_BOUNDARY)
                    .error(ProcessErrors.BUSINESS_ERROR)
                .serviceTask(PUBLISH_FAILURE)
                    .name("Publish failure")
                    .camundaDelegateExpression(delegate(PublishFailureDelegate.BEAN_NAME))
                .endEvent(FAILURE_END)
                .done();
    }

    public BpmnModelInstance timerModel() {
        return Bpmn.createExecutableProcess(TIMER_PROCESS_KEY)
                .name("Sales pending timer")
                .startEvent(TIMER_START)
                    .timerWithCycle(timerCycle)
                .serviceTask(CLAIM_PENDING_SALES)
                    .name("Claim pending sales")
                    .camundaDelegateExpression(delegate(ClaimPendingSalesDelegate.BEAN_NAME))
                .endEvent(TIMER_END)
                .done();
    }

    private static String delegate(String beanName) {
        return "${" + beanName + "}";
    }
}
