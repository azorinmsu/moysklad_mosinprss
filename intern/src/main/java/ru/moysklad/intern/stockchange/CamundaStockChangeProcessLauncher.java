package ru.moysklad.intern.stockchange;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.camunda.ProcessVariables;
import ru.moysklad.intern.camunda.StockChangeProcessDefinition;
import ru.moysklad.intern.dto.kafka.StockChangeCommand;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Component
public class CamundaStockChangeProcessLauncher implements StockChangeProcessLauncher {

    private final RuntimeService runtimeService;
    private final ObjectMapper objectMapper;

    public CamundaStockChangeProcessLauncher(RuntimeService runtimeService, ObjectMapper objectMapper) {
        this.runtimeService = runtimeService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void launch(StockChangeCommand command) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(ProcessVariables.REQUEST_ID, command.requestId().toString());
        variables.put(ProcessVariables.DOCUMENT_ID, command.documentId().toString());
        variables.put(ProcessVariables.DOCUMENT_TYPE, command.documentType().name());
        variables.put(ProcessVariables.MOMENT, command.moment().toString());
        variables.put(
                ProcessVariables.CORRELATION_ID,
                command.correlationId() == null ? command.requestId().toString() : command.correlationId().toString()
        );
        variables.put(ProcessVariables.POSITIONS_JSON, objectMapper.writeValueAsString(command.positions()));
        runtimeService.startProcessInstanceByKey(
                StockChangeProcessDefinition.PROCESS_KEY,
                command.requestId().toString(),
                variables
        );
    }
}
