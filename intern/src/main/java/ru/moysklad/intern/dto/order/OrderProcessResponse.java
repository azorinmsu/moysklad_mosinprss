package ru.moysklad.intern.dto.order;

import ru.moysklad.intern.entity.order.OrderProcessStage;
import ru.moysklad.intern.entity.order.OrderProcessState;
import ru.moysklad.intern.entity.order.OrderProcessStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderProcessResponse(
        UUID orderId,
        String processInstanceId,
        OrderProcessStatus status,
        OrderProcessStage currentStage,
        UUID demandId,
        boolean reserved,
        boolean irreversible,
        String errorReason,
        Instant updatedAt
) {
    public static OrderProcessResponse from(OrderProcessState state) {
        if (state == null) {
            return null;
        }
        return new OrderProcessResponse(
                state.getOrderId(),
                state.getProcessInstanceId(),
                state.getStatus(),
                state.getCurrentStage(),
                state.getDemandId(),
                state.isReserved(),
                state.isIrreversible(),
                state.getErrorReason(),
                state.getUpdatedAt()
        );
    }
}
