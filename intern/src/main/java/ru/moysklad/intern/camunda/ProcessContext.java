package ru.moysklad.intern.camunda;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import ru.moysklad.intern.dto.kafka.StockChangePositionDto;
import ru.moysklad.intern.entity.operation.DocumentsType;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Данные текущего шага процесса. Шаги студента читают контекст и при ошибке вызывают {@link #fail}.
 */
public class ProcessContext {

    private static final TypeReference<List<StockChangePositionDto>> POSITIONS_TYPE = new TypeReference<>() {
    };

    private final DelegateExecution execution;
    private final ObjectMapper objectMapper;

    public ProcessContext(DelegateExecution execution, ObjectMapper objectMapper) {
        this.execution = execution;
        this.objectMapper = objectMapper;
    }

    public UUID requestId() {
        return uuid(ProcessVariables.REQUEST_ID);
    }

    public UUID documentId() {
        return uuid(ProcessVariables.DOCUMENT_ID);
    }

    public DocumentsType documentType() {
        String value = string(ProcessVariables.DOCUMENT_TYPE);
        return value == null ? null : DocumentsType.valueOf(value);
    }

    public LocalDateTime moment() {
        String value = string(ProcessVariables.MOMENT);
        return value == null ? null : LocalDateTime.parse(value);
    }

    public UUID correlationId() {
        return uuid(ProcessVariables.CORRELATION_ID);
    }

    public List<StockChangePositionDto> positions() {
        String json = string(ProcessVariables.POSITIONS_JSON);
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return objectMapper.readValue(json, POSITIONS_TYPE);
    }

    public String errorCode() {
        return string(ProcessVariables.ERROR_CODE);
    }

    public String errorMessage() {
        return string(ProcessVariables.ERROR_MESSAGE);
    }

    public BigDecimal missingQuantity() {
        return decimal(ProcessVariables.MISSING_QUANTITY);
    }

    public BigDecimal missingAmount() {
        return decimal(ProcessVariables.MISSING_AMOUNT);
    }

    public void fail(String errorCode, String errorMessage) {
        ProcessErrors.fail(execution, errorCode, errorMessage);
    }

    public void fail(String errorCode, String errorMessage, BigDecimal missingQuantity, BigDecimal missingAmount) {
        ProcessErrors.fail(execution, errorCode, errorMessage, missingQuantity, missingAmount);
    }

    public DelegateExecution execution() {
        return execution;
    }

    private UUID uuid(String name) {
        String value = string(name);
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }

    private String string(String name) {
        Object value = execution.getVariable(name);
        return value == null ? null : value.toString();
    }

    private BigDecimal decimal(String name) {
        String value = string(name);
        return value == null || value.isBlank() ? null : new BigDecimal(value);
    }
}
