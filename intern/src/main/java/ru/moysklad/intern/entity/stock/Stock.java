package ru.moysklad.intern.entity.stock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ru.moysklad.intern.entity.operation.Documents;
import ru.moysklad.intern.entity.warehouse.Warehouse;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "serial_number_id", nullable = false)
    private SerialNumber serialNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Warehouse place;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Documents document;

    @Column(name = "moment", nullable = false)
    private LocalDateTime moment;

    @Column(name = "momentfrom", nullable = false)
    private LocalDateTime momentFrom;

    @Column(name = "momentto")
    private LocalDateTime momentTo;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Column(name = "delta", nullable = false)
    private int delta;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SerialNumber getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(SerialNumber serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Lot getLot() {
        return lot;
    }

    public void setLot(Lot lot) {
        this.lot = lot;
    }

    public Warehouse getPlace() {
        return place;
    }

    public void setPlace(Warehouse place) {
        this.place = place;
    }

    public Documents getDocument() {
        return document;
    }

    public void setDocument(Documents document) {
        this.document = document;
    }

    public LocalDateTime getMoment() {
        return moment;
    }

    public void setMoment(LocalDateTime moment) {
        this.moment = moment;
    }

    public LocalDateTime getMomentFrom() {
        return momentFrom;
    }

    public void setMomentFrom(LocalDateTime momentFrom) {
        this.momentFrom = momentFrom;
    }

    public LocalDateTime getMomentTo() {
        return momentTo;
    }

    public void setMomentTo(LocalDateTime momentTo) {
        this.momentTo = momentTo;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getDelta() {
        return delta;
    }

    public void setDelta(int delta) {
        this.delta = delta;
    }
}
