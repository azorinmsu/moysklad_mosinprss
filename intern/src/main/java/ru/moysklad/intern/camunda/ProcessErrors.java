package ru.moysklad.intern.camunda;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;

import java.math.BigDecimal;

/**
 * Бизнес-ошибка процесса. Переводит выполнение на шаг публикации неуспешного результата.
 */
public final class ProcessErrors {

    public static final String BUSINESS_ERROR = "BUSINESS_ERROR";

    private ProcessErrors() {
    }

    public static void fail(DelegateExecution execution, String errorCode, String errorMessage) {
        fail(execution, errorCode, errorMessage, null, null);
    }

    public static void fail(
            DelegateExecution execution,
            String errorCode,
            String errorMessage,
            BigDecimal missingQuantity,
            BigDecimal missingAmount
    ) {
        execution.setVariable(ProcessVariables.ERROR_CODE, errorCode);
        execution.setVariable(ProcessVariables.ERROR_MESSAGE, errorMessage);
        execution.setVariable(ProcessVariables.MISSING_QUANTITY, missingQuantity == null ? null : missingQuantity.toPlainString());
        execution.setVariable(ProcessVariables.MISSING_AMOUNT, missingAmount == null ? null : missingAmount.toPlainString());
        throw new BpmnError(BUSINESS_ERROR, errorMessage);
    }
}
