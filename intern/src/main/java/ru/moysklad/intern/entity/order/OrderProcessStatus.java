package ru.moysklad.intern.entity.order;

public enum OrderProcessStatus {
    PROCESSING,
    COMPLETED,
    REJECTED,
    TIMEOUT,
    STOCK_TIMEOUT,
    FAILED,
    CANCELLED,
    CANCEL_REFUSED,
    COMPENSATION_FAILED,
    FAILED_AFTER_POSTING
}
