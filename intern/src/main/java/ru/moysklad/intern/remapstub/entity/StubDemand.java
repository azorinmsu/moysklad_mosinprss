package ru.moysklad.intern.remapstub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "remap_stub_demand")
public class StubDemand {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean applicable;

    @Column(nullable = false)
    private LocalDateTime moment;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "customer_order_id")
    private UUID customerOrderId;

    @Version
    private long version;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isApplicable() {
        return applicable;
    }

    public void setApplicable(boolean applicable) {
        this.applicable = applicable;
    }

    public LocalDateTime getMoment() {
        return moment;
    }

    public void setMoment(LocalDateTime moment) {
        this.moment = moment;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public void setAgentId(UUID agentId) {
        this.agentId = agentId;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public void setStoreId(UUID storeId) {
        this.storeId = storeId;
    }

    public UUID getCustomerOrderId() {
        return customerOrderId;
    }

    public void setCustomerOrderId(UUID customerOrderId) {
        this.customerOrderId = customerOrderId;
    }

    public long getVersion() {
        return version;
    }
}
