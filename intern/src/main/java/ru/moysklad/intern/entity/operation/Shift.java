package ru.moysklad.intern.entity.operation;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * Перемещение. Списывает товар со склада-отправителя и приходует его на склад-получатель.
 */
@Entity
@Table(name = "shift")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("Shift")
public class Shift extends DocumentsWithPositions {

    @Override
    public DocumentsType getDocumentsType() {
        return DocumentsType.SHIFT;
    }
}
