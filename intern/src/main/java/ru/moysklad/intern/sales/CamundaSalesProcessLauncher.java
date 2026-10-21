package ru.moysklad.intern.sales;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.camunda.ProcessVariables;
import ru.moysklad.intern.camunda.SalesProcessDefinition;
import ru.moysklad.intern.dto.kafka.SalesCommand;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

@Component
public class CamundaSalesProcessLauncher implements SalesProcessLauncher {

    public static final String SALES_COMMAND_VARIABLE = "salesCommandJson";

    private final RuntimeService runtimeService;
    private final ObjectMapper objectMapper;

    public CamundaSalesProcessLauncher(RuntimeService runtimeService, ObjectMapper objectMapper) {
        this.runtimeService = runtimeService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void launch(SalesCommand command) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(ProcessVariables.REQUEST_ID, command.requestId().toString());
        variables.put(
                ProcessVariables.CORRELATION_ID,
                command.correlationId() == null ? command.requestId().toString() : command.correlationId().toString()
        );
        variables.put(SALES_COMMAND_VARIABLE, objectMapper.writeValueAsString(command));

        runtimeService.startProcessInstanceByKey(
                SalesProcessDefinition.PROCESS_KEY,
                command.requestId().toString(),
                variables
        );
    }
}
