package ru.moysklad.intern.remapstub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "remap_stub_fault_rule")
public class StubFaultRule {

    public enum FaultType {
        HTTP_STATUS,
        DELAY,
        TIMEOUT,
        CONNECTION_RESET,
        LOSE_RESPONSE
    }

    @Id
    private UUID id;

    @Column(nullable = false, length = 16)
    private String method;

    @Column(name = "path_pattern", nullable = false, length = 512)
    private String pathPattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "fault_type", nullable = false, length = 64)
    private FaultType faultType;

    @Column(name = "remaining_count", nullable = false)
    private int remainingCount;

    @Column(name = "delay_ms", nullable = false)
    private long delayMs;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    public FaultType getFaultType() {
        return faultType;
    }

    public void setFaultType(FaultType faultType) {
        this.faultType = faultType;
    }

    public int getRemainingCount() {
        return remainingCount;
    }

    public void setRemainingCount(int remainingCount) {
        this.remainingCount = remainingCount;
    }

    public long getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
