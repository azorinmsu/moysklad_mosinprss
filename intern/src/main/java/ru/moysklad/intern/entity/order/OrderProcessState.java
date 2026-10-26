package ru.moysklad.intern.entity.order;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_process_states")
public class OrderProcessState {
    // TODO: за заказы, если я правильно понимаю, отвечает именно [StubCustomerOrder]

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderProcessStatus status;

    @Column(name = "error_reason")
    private String errorReason;

    @Column(name = "irreversible", nullable = false)
    private boolean irreversible = false;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public OrderProcessStatus getStatus() {
        return status;
    }

    public void setStatus(OrderProcessStatus status) {
        this.status = status;
    }

    public String getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }

    public boolean isIrreversible() {
        return irreversible;
    }

    public void setIrreversible(boolean irreversible) {
        this.irreversible = irreversible;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
