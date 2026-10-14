package ru.moysklad.intern.entity.processing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "processing_result",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_processing_result_request_id",
                columnNames = "request_id"
        )
)
public class ProcessingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 64)
    private ProcessingResultStatus status;

    @Column(name = "error_code", length = 128)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "missing_quantity", precision = 19, scale = 6)
    private BigDecimal missingQuantity;

    @Column(name = "missing_amount", precision = 19, scale = 6)
    private BigDecimal missingAmount;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public ProcessingResult() {
    }

    @PrePersist
    void onCreate() {
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public void setRequestId(UUID requestId) {
        this.requestId = requestId;
    }

    public ProcessingResultStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessingResultStatus status) {
        this.status = status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public BigDecimal getMissingQuantity() {
        return missingQuantity;
    }

    public void setMissingQuantity(BigDecimal missingQuantity) {
        this.missingQuantity = missingQuantity;
    }

    public BigDecimal getMissingAmount() {
        return missingAmount;
    }

    public void setMissingAmount(BigDecimal missingAmount) {
        this.missingAmount = missingAmount;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
