package ru.moysklad.intern.camunda;

public final class ProcessVariables {

    public static final String REQUEST_ID = "requestId";
    public static final String DOCUMENT_ID = "documentId";
    public static final String DOCUMENT_TYPE = "documentType";
    public static final String MOMENT = "moment";
    public static final String CORRELATION_ID = "correlationId";
    public static final String POSITIONS_JSON = "positionsJson";
    public static final String STATUS = "status";
    public static final String ERROR_CODE = "errorCode";
    public static final String ERROR_MESSAGE = "errorMessage";
    public static final String MISSING_QUANTITY = "missingQuantity";
    public static final String MISSING_AMOUNT = "missingAmount";

    // под заказы
    public static final String ORDER_ID = "orderId";
    public static final String STOCK_SUFFICIENT = "stockSufficient";
    public static final String STOCK_WAIT_EXPIRED = "stockWaitExpired";
    public static final String DEMAND_ID = "demandId";
    public static final String IRREVERSIBLE = "irreversible";
    public static final String FINAL_STATUS = "finalStatus";
    public static final String ERROR_REASON = "errorReason";
    public static final String CURRENT_STAGE = "currentStage";
    public static final String OUTCOME = "outcome";
    public static final String STOCK_RETRY_COUNT = "stockRetryCount";
    public static final String RESERVED = "reserved";
    public static final String COMPENSATION_STATUS = "compensationStatus";

    private ProcessVariables() {
    }
}
