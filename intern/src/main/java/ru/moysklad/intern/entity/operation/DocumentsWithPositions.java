package ru.moysklad.intern.entity.operation;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.stock.PositionType;
import ru.moysklad.intern.entity.stock.Positions;
import ru.moysklad.intern.entity.stock.SerialNumber;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "documents_with_positions")
@PrimaryKeyJoinColumn(name = "id")
public abstract class DocumentsWithPositions extends Documents {

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Positions> motions = new ArrayList<>();

    @Transient
    private PreviousState snapshot;

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

    /** Состояние до текущего сохранения — для отката старого вклада при проверке остатков. */
    public PreviousState previousState() {
        return snapshot;
    }

    public void captureSnapshot() {
        snapshot = buildSnapshot();
    }

    public void copySnapshotFrom(DocumentsWithPositions other) {
        this.snapshot = other == null ? null : other.snapshot;
    }

    private PreviousState buildSnapshot() {
        List<PrevThing> things = new ArrayList<>();
        for (Positions motion : motions) {
            PositionType motionType = motion.getPositionType();
            for (SerialNumber serial : motion.getSerialNumbers()) {
                if (serial.getId() != null) {
                    things.add(new PrevThing(serial.getId(), motionType));
                }
            }
        }
        return new PreviousState(
                isApplicable(),
                getMoment(),
                getSourceStore() == null ? null : getSourceStore().getId(),
                getTargetStore() == null ? null : getTargetStore().getId(),
                List.copyOf(things)
        );
    }

    public record PreviousState(
            boolean applicable,
            LocalDateTime moment,
            UUID sourceStoreId,
            UUID targetStoreId,
            List<PrevThing> things
    ) {
    }

    public record PrevThing(UUID thingId, PositionType motionType) {
    }
}
