package ru.moysklad.intern.camunda;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.camunda.delegate.CheckAvailableStockDelegate;
import ru.moysklad.intern.camunda.delegate.CheckSerialNumbersDelegate;
import ru.moysklad.intern.camunda.delegate.PublishFailureDelegate;
import ru.moysklad.intern.camunda.delegate.PublishResultDelegate;
import ru.moysklad.intern.camunda.delegate.UpdateStockAndPriceDelegate;

/**
 * Процесс изменения остатков. Порядок шагов задаётся этим классом, BPMN-файл не используется.
 */
@Component
public class StockChangeProcessDefinition {

    public static final String PROCESS_KEY = "stock-change";

    public static final String START = "START";
    public static final String STEPS = "STOCK_CHANGE_STEPS";
    public static final String CHECK_SERIAL_NUMBERS = "CHECK_SERIAL_NUMBERS";
    public static final String CHECK_AVAILABLE_STOCK = "CHECK_AVAILABLE_STOCK";
    public static final String UPDATE_STOCK_AND_PRICE = "UPDATE_STOCK_AND_PRICE";
    public static final String PUBLISH_RESULT = "PUBLISH_RESULT";
    public static final String END = "END";
    public static final String BUSINESS_ERROR_BOUNDARY = "STOCK_CHANGE_BUSINESS_ERROR";
    public static final String PUBLISH_FAILURE = "PUBLISH_FAILURE";
    public static final String FAILURE_END = "FAILURE_END";

    public BpmnModelInstance model() {
        return Bpmn.createExecutableProcess(PROCESS_KEY)
                .name("Stock change")
                .startEvent(START)
                .subProcess(STEPS)
                    .embeddedSubProcess()
                        .startEvent("STOCK_CHANGE_STEPS_START")
                        .serviceTask(CHECK_SERIAL_NUMBERS)
                            .name("Check serial numbers")
                            .camundaDelegateExpression(delegate(CheckSerialNumbersDelegate.BEAN_NAME))
                        .serviceTask(CHECK_AVAILABLE_STOCK)
                            .name("Check available stock")
                            .camundaDelegateExpression(delegate(CheckAvailableStockDelegate.BEAN_NAME))
                        .serviceTask(UPDATE_STOCK_AND_PRICE)
                            .name("Update stock and price")
                            .camundaDelegateExpression(delegate(UpdateStockAndPriceDelegate.BEAN_NAME))
                        .endEvent("STOCK_CHANGE_STEPS_END")
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

    private static String delegate(String beanName) {
        return "${" + beanName + "}";
    }
}
