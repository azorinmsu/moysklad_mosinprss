package ru.moysklad.intern.entity.operation;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import ru.moysklad.intern.entity.stock.PositionType;
import ru.moysklad.intern.entity.stock.Positions;

import java.util.List;

/**
 * Выполнение этапа производства. Списывает материалы этапа и приходует готовую продукцию.
 */
@Entity
@Table(name = "make")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("Make")
public class Make extends DocumentsWithPositions {

    @Override
    public DocumentsType getDocumentsType() {
        return DocumentsType.MAKE;
    }

    @Override
    public List<Positions> getProducts() {
        return getMotions().stream()
                .filter(motion -> motion.getPositionType() == PositionType.RESULT)
                .toList();
    }
}
