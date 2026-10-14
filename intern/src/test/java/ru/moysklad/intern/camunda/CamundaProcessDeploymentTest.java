package ru.moysklad.intern.camunda;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BoundaryEvent;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.moysklad.intern.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class CamundaProcessDeploymentTest {

    @Autowired
    private RepositoryService repositoryService;

    @Test
    void deploysStockChangeProcessWithProgrammaticSteps() {
        ProcessDefinition definition = latest(StockChangeProcessDefinition.PROCESS_KEY);
        BpmnModelInstance model = repositoryService.getBpmnModelInstance(definition.getId());

        assertThat(serviceTask(model, StockChangeProcessDefinition.CHECK_SERIAL_NUMBERS).getCamundaDelegateExpression())
                .isEqualTo("${checkSerialNumbersDelegate}");
        assertThat(serviceTask(model, StockChangeProcessDefinition.CHECK_AVAILABLE_STOCK).getCamundaDelegateExpression())
                .isEqualTo("${checkAvailableStockDelegate}");
        assertThat(serviceTask(model, StockChangeProcessDefinition.UPDATE_STOCK_AND_PRICE).getCamundaDelegateExpression())
                .isEqualTo("${updateStockAndPriceDelegate}");
        assertThat(serviceTask(model, StockChangeProcessDefinition.PUBLISH_RESULT).getCamundaDelegateExpression())
                .isEqualTo("${publishResultDelegate}");
        assertThat(serviceTask(model, StockChangeProcessDefinition.PUBLISH_FAILURE).getCamundaDelegateExpression())
                .isEqualTo("${publishFailureDelegate}");
        assertThat((Object) model.getModelElementById(StockChangeProcessDefinition.BUSINESS_ERROR_BOUNDARY))
                .isInstanceOf(BoundaryEvent.class);
    }

    @Test
    void deploysSalesProcessAndPendingTimer() {
        ProcessDefinition sales = latest(SalesProcessDefinition.PROCESS_KEY);
        BpmnModelInstance salesModel = repositoryService.getBpmnModelInstance(sales.getId());
        assertThat(serviceTask(salesModel, SalesProcessDefinition.CHECK_SALES_STOCK).getCamundaDelegateExpression())
                .isEqualTo("${checkSalesStockDelegate}");
        assertThat(serviceTask(salesModel, SalesProcessDefinition.CHECK_AVAILABLE_AMOUNT).getCamundaDelegateExpression())
                .isEqualTo("${checkAvailableAmountDelegate}");
        assertThat(serviceTask(salesModel, SalesProcessDefinition.WRITE_OFF_STOCK).getCamundaDelegateExpression())
                .isEqualTo("${writeOffStockDelegate}");

        ProcessDefinition timer = latest(SalesProcessDefinition.TIMER_PROCESS_KEY);
        BpmnModelInstance timerModel = repositoryService.getBpmnModelInstance(timer.getId());
        assertThat((Object) timerModel.getModelElementById(SalesProcessDefinition.TIMER_START)).isNotNull();
        assertThat(serviceTask(timerModel, SalesProcessDefinition.CLAIM_PENDING_SALES).getCamundaDelegateExpression())
                .isEqualTo("${claimPendingSalesDelegate}");
    }

    private ProcessDefinition latest(String key) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(key)
                .latestVersion()
                .singleResult();
        assertThat(definition).isNotNull();
        return definition;
    }

    private static ServiceTask serviceTask(BpmnModelInstance model, String id) {
        ServiceTask task = model.getModelElementById(id);
        assertThat(task).isNotNull();
        return task;
    }
}
