package ru.moysklad.intern.entity.order;

public enum OrderProcessStage {
    VALIDATE_ORDER,
    CHECK_STOCK,
    RESERVE_STOCK,
    WAIT_CONFIRMATION,
    CREATE_DEMAND,
    VALIDATE_DEMAND,
    CONFIRM_DEMAND, // т. невозврата
    COMPLETE_ORDER,
    COMPENSATING
}
