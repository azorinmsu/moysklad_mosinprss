package ru.moysklad.intern.entity.operation;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "dtype")
public abstract class Documents {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "moment", nullable = false)
    private LocalDateTime moment;

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(name = "applicable", nullable = false)
    private boolean applicable = true;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "source_store_id")
    private Warehouse sourceStore;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "target_store_id")
    private Warehouse targetStore;

    @PrePersist
    void onCreate() {
        if (created == null) {
            created = LocalDateTime.now();
        }
        if (moment == null) {
            moment = created;
        }
    }

    public abstract DocumentsType getDocumentsType();

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

    public LocalDateTime getMoment() {
        return moment;
    }

    public void setMoment(LocalDateTime moment) {
        this.moment = moment;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public boolean isApplicable() {
        return applicable;
    }

    public void setApplicable(boolean applicable) {
        this.applicable = applicable;
    }

    public Warehouse getSourceStore() {
        return sourceStore;
    }

    public void setSourceStore(Warehouse sourceStore) {
        this.sourceStore = sourceStore;
    }

    public Warehouse getTargetStore() {
        return targetStore;
    }

    public void setTargetStore(Warehouse targetStore) {
        this.targetStore = targetStore;
    }
}
