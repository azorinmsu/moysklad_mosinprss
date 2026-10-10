package ru.moysklad.intern.entity.operation;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.stock.PositionType;
import ru.moysklad.intern.entity.stock.Positions;
import ru.moysklad.intern.entity.stock.SerialNumber;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documents_with_positions")
@PrimaryKeyJoinColumn(name = "id")
public abstract class DocumentsWithPositions extends Documents {

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Positions> motions = new ArrayList<>();

    public List<Positions> getMotions() {
        return motions;
    }

    public List<Positions> getPositions() {
        return motions.stream()
                .filter(motion -> motion.getPositionType() != PositionType.RESULT)
                .toList();
    }

    public List<Positions> getProducts() {
        return List.of();
    }

    public Positions addMotion(Lot lot, SerialNumber serialNumber, PositionType positionType) {
        Positions position = new Positions();
        position.setDocument(this);
        position.setLot(lot);
        position.setPositionType(positionType);
        if (serialNumber != null) {
            position.getSerialNumbers().add(serialNumber);
        }
        motions.add(position);
        return position;
    }
}
