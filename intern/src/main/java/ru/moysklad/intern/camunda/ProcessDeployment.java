package ru.moysklad.intern.camunda;

import org.camunda.bpm.engine.RepositoryService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class ProcessDeployment implements ApplicationRunner {

    private final RepositoryService repositoryService;
    private final StockChangeProcessDefinition stockChangeProcessDefinition;
    private final SalesProcessDefinition salesProcessDefinition;

    public ProcessDeployment(
            RepositoryService repositoryService,
            StockChangeProcessDefinition stockChangeProcessDefinition,
            SalesProcessDefinition salesProcessDefinition
    ) {
        this.repositoryService = repositoryService;
        this.stockChangeProcessDefinition = stockChangeProcessDefinition;
        this.salesProcessDefinition = salesProcessDefinition;
    }

    @Override
    public void run(ApplicationArguments args) {
        repositoryService.createDeployment()
                .name("intern-processes")
                .enableDuplicateFiltering(true)
                .addModelInstance("stock-change.bpmn", stockChangeProcessDefinition.model())
                .addModelInstance("sales.bpmn", salesProcessDefinition.model())
                .addModelInstance("sales-pending-timer.bpmn", salesProcessDefinition.timerModel())
                .deploy();
    }
}
