package ru.moysklad.intern.entity.operation;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import ru.moysklad.intern.entity.stock.PositionType;
import ru.moysklad.intern.entity.stock.Positions;

import java.util.List;

/**
 * Техоперация. Списывает материалы и приходует выпущенную продукцию.
 */
@Entity
@Table(name = "machining")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("Machining")
public class Machining extends DocumentsWithPositions {

    @Override
    public DocumentsType getDocumentsType() {
        return DocumentsType.MACHINING;
    }

    @Override
    public List<Positions> getProducts() {
        return getMotions().stream()
                .filter(motion -> motion.getPositionType() == PositionType.RESULT)
                .toList();
    }
}
