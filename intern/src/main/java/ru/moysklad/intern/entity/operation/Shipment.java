package ru.moysklad.intern.entity.operation;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Отгрузка. Списывает товар со склада при продаже покупателю.
 */
@Entity
@Table(name = "shipment")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("Shipment")
public class Shipment extends DocumentsWithPositions {

    @Override
    public DocumentsType getDocumentsType() {
        return DocumentsType.SHIPMENT;
    }
}
