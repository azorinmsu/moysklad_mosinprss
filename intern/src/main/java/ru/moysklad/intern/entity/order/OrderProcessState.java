package ru.moysklad.intern.entity.order;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_process_states")
public class OrderProcessState {
    // TODO: за заказы, если я правильно понимаю, отвечает именно [StubCustomerOrder]

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "process_instance_id")
    private String processInstanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage")
    private OrderProcessStage currentStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderProcessStatus status = OrderProcessStatus.PROCESSING;

    @Column(name = "demand_id")
    private UUID demandId;

    @Column(name = "reserved", nullable = false)
    private boolean reserved = false;

    @Column(name = "error_reason")
    private String errorReason;

    @Column(name = "irreversible", nullable = false)
    private boolean irreversible = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public UUID getDemandId() {
        return demandId;
    }

    public void setDemandId(UUID demandId) {
        this.demandId = demandId;
    }

    public OrderProcessStage getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(OrderProcessStage currentStage) {
        this.currentStage = currentStage;
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
