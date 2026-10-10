package ru.moysklad.intern.service.thing;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.entity.operation.DocumentsType;
import ru.moysklad.intern.entity.operation.DocumentsWithPositions;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.stock.Positions;
import ru.moysklad.intern.entity.stock.SerialNumber;
import ru.moysklad.intern.entity.stock.Stock;
import ru.moysklad.intern.entity.warehouse.Warehouse;
import ru.moysklad.intern.exception.ThingAlreadyInStockException;
import ru.moysklad.intern.exception.ThingNotInStockException;
import ru.moysklad.intern.exception.ThingStockValidationException;
import ru.moysklad.intern.repo.DocumentsRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import static ru.moysklad.intern.service.thing.SerialCheckStore.ALL_PLACES;
import static ru.moysklad.intern.service.thing.SerialCheckStore.EXCLUDE_PLACE;
import static ru.moysklad.intern.service.thing.SerialCheckStore.ONLY_PLACE;

@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ThingStockManager {

    private final DocumentsRepository documentsRepository;
    private final EntityManager entityManager;

    public ThingStockManager(DocumentsRepository documentsRepository, EntityManager entityManager) {
        this.documentsRepository = documentsRepository;
        this.entityManager = entityManager;
    }

    /**
     * Проводит документ, который приходует серийные номера: приемку, перемещение,
     * техоперацию или выполнение этапа. Остаток на складе-получателе увеличивается.
     */
    public void addSerialsToWarehouse(DocumentsWithPositions operation) {
        if (!receivesSerials(operation)) {
            throw new IllegalArgumentException("Документ не приходует серийные номера: " + operation.getDocumentsType());
        }
        apply(operation);
    }

    /**
     * Проводит документ, который списывает серийные номера: отгрузку, перемещение,
     * техоперацию или выполнение этапа. Остаток на складе-отправителе уменьшается.
     */
    public void writeOffSerialsFromWarehouse(DocumentsWithPositions operation) {
        if (!issuesSerials(operation)) {
            throw new IllegalArgumentException("Документ не списывает серийные номера: " + operation.getDocumentsType());
        }
        apply(operation);
    }

    /**
     * Удаляет документ и пересчитывает остатки серийных номеров без него.
     */
    public void removeOperation(DocumentsWithPositions operation) {
        if (operation.getId() == null) {
            return;
        }
        deleteStocks();
        DocumentsWithPositions managed = entityManager.find(DocumentsWithPositions.class, operation.getId());
        if (managed != null) {
            entityManager.remove(managed);
            entityManager.flush();
        }
        recalculateAll();
    }


    /**
     * Проверяет серийные номера пакета документов. Документы одного типа сверяются вместе,
     * чтобы один серийник не приходовался и не списывался дважды.
     */
    public void checkThingStocks(Collection<? extends DocumentsWithPositions> operations) throws ThingStockValidationException {
        Map<DocumentsType, List<DocumentsWithPositions>> operationsByType = new HashMap<>();
        for (DocumentsWithPositions operation : operations) {
            operationsByType.computeIfAbsent(operation.getDocumentsType(), type -> new ArrayList<>()).add(operation);
        }
        for (List<DocumentsWithPositions> grouped : operationsByType.values()) {
            checkThingStocksInternal(grouped);
        }
    }

    private void apply(DocumentsWithPositions operation) {
        validateStores(operation);
        if (operation.getMoment() == null) {
            throw new IllegalArgumentException("У документа не указана дата");
        }
        saveDocs(operation);
        recalculateAll();
    }

    private DocumentsWithPositions saveDocs(DocumentsWithPositions operation) {
        attachExistingReferences(operation);
        if (operation.getId() == null) {
            entityManager.persist(operation);
            entityManager.flush();
            return operation;
        }
        DocumentsWithPositions merged = entityManager.merge(operation);
        entityManager.flush();
        copyGeneratedIds(operation, merged);
        return merged;
    }

    private void copyGeneratedIds(DocumentsWithPositions source, DocumentsWithPositions managed) {
        copyStoreId(source.getSourceStore(), managed.getSourceStore());
        copyStoreId(source.getTargetStore(), managed.getTargetStore());
        List<Positions> sourcePositions = source.getMotions();
        List<Positions> managedPositions = managed.getMotions();
        int limit = Math.min(sourcePositions.size(), managedPositions.size());
        for (int index = 0; index < limit; index++) {
            Positions sourcePosition = sourcePositions.get(index);
            Positions managedPosition = managedPositions.get(index);
            if (sourcePosition.getId() == null) {
                sourcePosition.setId(managedPosition.getId());
            }
            if (sourcePosition.getLot() != null
                    && sourcePosition.getLot().getId() == null
                    && managedPosition.getLot() != null) {
                sourcePosition.getLot().setId(managedPosition.getLot().getId());
            }
            List<SerialNumber> sourceSerials = new ArrayList<>(sourcePosition.getSerialNumbers());
            List<SerialNumber> managedSerials = new ArrayList<>(managedPosition.getSerialNumbers());
            int serialLimit = Math.min(sourceSerials.size(), managedSerials.size());
            for (int serialIndex = 0; serialIndex < serialLimit; serialIndex++) {
                if (sourceSerials.get(serialIndex).getId() == null) {
                    sourceSerials.get(serialIndex).setId(managedSerials.get(serialIndex).getId());
                }
            }
        }
    }

    private void copyStoreId(Warehouse source, Warehouse managed) {
        if (source != null && source.getId() == null && managed != null) {
            source.setId(managed.getId());
        }
    }

    private void attachExistingReferences(DocumentsWithPositions operation) {
        operation.setSourceStore(attach(operation.getSourceStore()));
        operation.setTargetStore(attach(operation.getTargetStore()));
        for (Positions position : operation.getMotions()) {
            Lot lot = attach(position.getLot());
            position.setLot(lot);
            Set<SerialNumber> serials = new HashSet<>();
            for (SerialNumber serial : new HashSet<>(position.getSerialNumbers())) {
                if (serial.getId() != null) {
                    serials.add(entityManager.getReference(SerialNumber.class, serial.getId()));
                } else {
                    if (serial.getLot() != null && serial.getLot().getId() != null) {
                        serial.setLot(entityManager.getReference(Lot.class, serial.getLot().getId()));
                    } else if (lot != null) {
                        serial.setLot(lot);
                    }
                    serials.add(serial);
                }
            }
            position.setSerialNumbers(serials);
        }
    }

    private Warehouse attach(Warehouse warehouse) {
        if (warehouse == null || warehouse.getId() == null) {
            return warehouse;
        }
        return entityManager.getReference(Warehouse.class, warehouse.getId());
    }

    private Lot attach(Lot lot) {
        if (lot == null || lot.getId() == null) {
            return lot;
        }
        return entityManager.getReference(Lot.class, lot.getId());
    }

    private void validateStores(DocumentsWithPositions operation) {
        switch (operation.getDocumentsType()) {
            case GOODS_ACCEPTANCE -> requireStore(operation.getTargetStore(), "У приемки не указан склад");
            case SHIPMENT -> requireStore(operation.getSourceStore(), "У отгрузки не указан склад");
            case SHIFT -> {
                requireStore(operation.getSourceStore(), "У перемещения не указан склад-отправитель");
                requireStore(operation.getTargetStore(), "У перемещения не указан склад-получатель");
            }
            case MACHINING, MAKE -> {
                requireStore(operation.getSourceStore(), "У производственной операции не указан склад материалов");
                requireStore(operation.getTargetStore(), "У производственной операции не указан склад продукции");
            }
        }
    }

    private void requireStore(Warehouse warehouse, String message) {
        if (warehouse == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private boolean receivesSerials(DocumentsWithPositions operation) {
        return switch (operation.getDocumentsType()) {
            case GOODS_ACCEPTANCE, SHIFT, MACHINING, MAKE -> true;
            case SHIPMENT -> false;
        };
    }

    private boolean issuesSerials(DocumentsWithPositions operation) {
        return switch (operation.getDocumentsType()) {
            case SHIPMENT, SHIFT, MACHINING, MAKE -> true;
            case GOODS_ACCEPTANCE -> false;
        };
    }

    private void checkThingStocksInternal(Collection<DocumentsWithPositions> operations) throws ThingStockValidationException {

    }

    private List<StockView> getStocks(LocalDateTime moment,
                                      Set<String> serials,
                                      UUID operationId,
                                      UUID lotId,
                                      UUID placeId,
                                      SerialCheckStore checkStore,
                                      boolean excludeOperation) {
        if (checkStore != ALL_PLACES && placeId == null) {
            throw new IllegalArgumentException("Для проверки остатка серийника нужен склад");
        }
        Set<String> normalized = new HashSet<>();
        for (String serial : serials) {
            if (serial != null && !serial.isBlank()) {
                normalized.add(serial.trim());
            }
        }
        List<StockView> found = normalized.isEmpty()
                ? List.of()
                : loadLatestStocks(moment, normalized, lotId, placeId, checkStore, excludeOperation ? operationId : null);

        Set<String> present = new HashSet<>();
        for (StockView stock : found) {
            present.add(stock.serial);
        }
        List<StockView> result = new ArrayList<>(found);
        for (String serial : normalized) {
            if (!present.contains(serial)) {
                result.add(new StockView(null, serial, null, 0));
            }
        }
        return result;
    }

    private List<StockView> loadLatestStocks(LocalDateTime moment,
                                             Set<String> serials,
                                             UUID lotId,
                                             UUID placeId,
                                             SerialCheckStore checkStore,
                                             UUID excludedOperationId) {
        List<Stock> rows = entityManager.createQuery("""
                        select s from Stock s
                        join fetch s.serialNumber sn
                        where sn.name in :serials
                          and s.lot.id = :lotId
                          and s.momentFrom <= :moment
                        """, Stock.class)
                .setParameter("serials", serials)
                .setParameter("lotId", lotId)
                .setParameter("moment", moment)
                .getResultList();

        Map<StockIdentity, Stock> latest = new HashMap<>();
        for (Stock row : rows) {
            if (excludedOperationId != null && excludedOperationId.equals(row.getDocument().getId())) {
                continue;
            }
            UUID rowPlaceId = row.getPlace().getId();
            if (checkStore == ONLY_PLACE && !placeId.equals(rowPlaceId)) {
                continue;
            }
            if (checkStore == EXCLUDE_PLACE && placeId.equals(rowPlaceId)) {
                continue;
            }
            StockIdentity identity = new StockIdentity(row.getSerialNumber().getId(), rowPlaceId, lotId);
            Stock current = latest.get(identity);
            if (current == null || isLater(row, current)) {
                latest.put(identity, row);
            }
        }

        List<StockView> result = new ArrayList<>();
        for (Stock row : latest.values()) {
            result.add(new StockView(
                    row.getSerialNumber().getId(),
                    row.getSerialNumber().getName(),
                    row.getPlace().getId(),
                    row.getStock()
            ));
        }
        return result;
    }

    private boolean isLater(Stock candidate, Stock current) {
        int byMomentFrom = candidate.getMomentFrom().compareTo(current.getMomentFrom());
        if (byMomentFrom != 0) {
            return byMomentFrom > 0;
        }
        return candidate.getMoment().isAfter(current.getMoment());
    }


    private Map<UUID, Set<String>> lotSerials(DocumentsWithPositions operation) {
        Map<UUID, Set<String>> result = new HashMap<>();
        for (Positions position : operation.getPositions()) {
            if (position.getLot() == null || position.getLot().getId() == null) {
                continue;
            }
            Set<String> serials = result.computeIfAbsent(position.getLot().getId(), ignored -> new HashSet<>());
            for (SerialNumber serial : position.getSerialNumbers()) {
                if (serial.getName() != null && !serial.getName().isBlank()) {
                    serials.add(serial.getName().trim());
                }
            }
        }
        return result;
    }

    private UUID sourceStoreId(DocumentsWithPositions operation) {
        return operation.getSourceStore() == null ? null : operation.getSourceStore().getId();
    }

    private SerialCheckStore getSerialCheck(DocumentsWithPositions operation) {
        return operation.getDocumentsType() == DocumentsType.GOODS_ACCEPTANCE ? ALL_PLACES : ONLY_PLACE;
    }

    private boolean checkOutputOperation(SerialCheckStore checkStore) {
        return checkStore == ONLY_PLACE;
    }

    private boolean checkInputOperation(SerialCheckStore checkStore) {
        return checkStore == ALL_PLACES || checkStore == EXCLUDE_PLACE;
    }

    private SerialKey serialKey(String serial, UUID lotId, UUID storeId) {
        return new SerialKey(serial == null ? null : serial.trim(), lotId, storeId);
    }

    private void throwThingStockValidationException(List<IncorrectThing> incorrectThings) {
        throwThingStockValidationException(incorrectThings, IncorrectType.ALREADY_IN_STOCK);
        throwThingStockValidationException(incorrectThings, IncorrectType.NOT_IN_STOCK);
    }

    private void throwThingStockValidationException(List<IncorrectThing> incorrectThings, IncorrectType type) {
        Set<String> serials = new HashSet<>();
        for (IncorrectThing incorrectThing : incorrectThings) {
            if (incorrectThing.type == type) {
                serials.add(incorrectThing.name);
            }
        }
        if (!serials.isEmpty()) {
            if (type == IncorrectType.ALREADY_IN_STOCK) {
                throw new ThingAlreadyInStockException(serials);
            }
            throw new ThingNotInStockException(serials);
        }
    }

    private void recalculateAll() {
        deleteStocks();
        entityManager.clear();
        Map<UUID, DocumentsWithPositions> unique = new LinkedHashMap<>();
        for (DocumentsWithPositions operation : documentsRepository.findApplicable()) {
            unique.put(operation.getId(), operation);
        }
        for (Stock stock : buildRows(unique.values())) {
            entityManager.persist(stock);
        }
        entityManager.flush();
    }

    private void deleteStocks() {
        entityManager.createQuery("delete from Stock s").executeUpdate();
        entityManager.flush();
    }

    private List<Stock> buildRows(Collection<DocumentsWithPositions> operations) {
        List<StockEvent> events = new ArrayList<>();
        for (DocumentsWithPositions operation : operations) {
            if (operation.isApplicable()) {
                events.addAll(eventsOf(operation));
            }
        }
        Map<StockIdentity, List<StockEvent>> grouped = new HashMap<>();
        for (StockEvent event : events) {
            grouped.computeIfAbsent(new StockIdentity(event.serial.getId(), event.place.getId(), event.lot.getId()), ignored -> new ArrayList<>())
                    .add(event);
        }

        List<Stock> rows = new ArrayList<>();
        for (List<StockEvent> placeEvents : grouped.values()) {
            placeEvents.sort(Comparator.comparing(StockEvent::moment)
                    .thenComparing(StockEvent::created, Comparator.nullsLast(Comparator.naturalOrder())));
            for (int index = 0; index < placeEvents.size(); index++) {
                StockEvent event = placeEvents.get(index);
                int stock = 0;
                for (StockEvent other : placeEvents) {
                    if (!other.moment.isAfter(event.moment)) {
                        stock += other.delta;
                    }
                }
                LocalDateTime momentTo = index + 1 < placeEvents.size() ? placeEvents.get(index + 1).moment : null;
                rows.add(toStock(event, stock, momentTo));
            }
        }
        return rows;
    }

    private List<StockEvent> eventsOf(DocumentsWithPositions operation) {
        initializeSerials(operation);
        return switch (operation.getDocumentsType()) {
            case GOODS_ACCEPTANCE -> incoming(operation, operation.getPositions(), operation.getTargetStore());
            case SHIPMENT -> outgoing(operation, operation.getPositions(), operation.getSourceStore());
            case SHIFT -> shiftEvents(operation);
            case MACHINING, MAKE -> productionEvents(operation);
        };
    }

    private List<StockEvent> shiftEvents(DocumentsWithPositions operation) {
        if (sameStore(operation.getSourceStore(), operation.getTargetStore())) {
            return List.of();
        }
        List<StockEvent> events = new ArrayList<>();
        events.addAll(outgoing(operation, operation.getPositions(), operation.getSourceStore()));
        events.addAll(incoming(operation, operation.getPositions(), operation.getTargetStore()));
        return events;
    }

    private List<StockEvent> productionEvents(DocumentsWithPositions operation) {
        List<StockEvent> events = new ArrayList<>();
        events.addAll(outgoing(operation, operation.getPositions(), operation.getSourceStore()));
        events.addAll(incoming(operation, operation.getProducts(), operation.getTargetStore()));
        return events;
    }

    private List<StockEvent> incoming(DocumentsWithPositions operation, List<Positions> positions, Warehouse place) {
        return deltas(operation, positions, place, 1);
    }

    private List<StockEvent> outgoing(DocumentsWithPositions operation, List<Positions> positions, Warehouse place) {
        return deltas(operation, positions, place, -1);
    }

    private List<StockEvent> deltas(DocumentsWithPositions operation, List<Positions> positions, Warehouse place, int delta) {
        if (place == null) {
            return List.of();
        }
        List<StockEvent> events = new ArrayList<>();
        for (Positions position : positions) {
            for (SerialNumber serial : position.getSerialNumbers()) {
                events.add(new StockEvent(
                        serial,
                        position.getLot(),
                        place,
                        operation,
                        operation.getMoment(),
                        operation.getCreated(),
                        delta
                ));
            }
        }
        return events;
    }

    private void initializeSerials(DocumentsWithPositions operation) {
        for (Positions position : operation.getMotions()) {
            position.getSerialNumbers().size();
        }
    }

    private boolean sameStore(Warehouse left, Warehouse right) {
        return left != null && right != null && Objects.equals(left.getId(), right.getId());
    }

    private Stock toStock(StockEvent event, int stock, LocalDateTime momentTo) {
        Stock row = new Stock();
        row.setSerialNumber(event.serial);
        row.setLot(event.lot);
        row.setPlace(event.place);
        row.setDocument(event.operation);
        row.setMoment(event.moment);
        row.setMomentFrom(event.moment);
        row.setMomentTo(momentTo);
        row.setStock(stock);
        row.setDelta(event.delta);
        return row;
    }

    private record SerialKey(String serial, UUID lotId, UUID storeId) {
    }

    private record StoreAndLot(UUID lotId, UUID storeId) {
    }

    private record OperationAndLot(UUID operationId, UUID lotId) {
    }

    private record StockIdentity(UUID serialId, UUID placeId, UUID lotId) {
    }

    private record StockEvent(
            SerialNumber serial,
            Lot lot,
            Warehouse place,
            DocumentsWithPositions operation,
            LocalDateTime moment,
            LocalDateTime created,
            int delta
    ) {
    }

    private enum IncorrectType {
        ALREADY_IN_STOCK,
        NOT_IN_STOCK
    }

    private record IncorrectThing(String name, IncorrectType type) {
    }

    private static final class StockView {
        private final UUID serialId;
        private final String serial;
        private final UUID placeId;
        private int stock;

        private StockView(UUID serialId, String serial, UUID placeId, int stock) {
            this.serialId = serialId;
            this.serial = serial;
            this.placeId = placeId;
            this.stock = stock;
        }
    }
}
