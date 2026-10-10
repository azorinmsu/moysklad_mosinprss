package ru.moysklad.intern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import jakarta.persistence.EntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import ru.moysklad.intern.entity.operation.DocumentsWithPositions;
import ru.moysklad.intern.entity.operation.GoodsAcceptance;
import ru.moysklad.intern.entity.operation.Machining;
import ru.moysklad.intern.entity.operation.Make;
import ru.moysklad.intern.entity.operation.Shift;
import ru.moysklad.intern.entity.operation.Shipment;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.stock.PositionType;
import ru.moysklad.intern.entity.stock.SerialNumber;
import ru.moysklad.intern.entity.stock.Stock;
import ru.moysklad.intern.entity.warehouse.Warehouse;
import ru.moysklad.intern.exception.ThingAlreadyInStockException;
import ru.moysklad.intern.exception.ThingNotInStockException;
import ru.moysklad.intern.repo.StockRepository;
import ru.moysklad.intern.service.thing.ThingStockManager;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ThingStockTest {

    @Autowired
    private ThingStockManager thingStockManager;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private EntityManager entityManager;

    private LocalDateTime startMoment;
    private Warehouse store;
    private Warehouse store2;
    private Lot lot;
    private SerialNumber serialNumber;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.execute("""
                truncate table stock, positions_serial_number, positions, goods_acceptance, shipment, shift, machining,
                    make, documents_with_positions, documents, serial_number, lot, warehouse
                """));
        startMoment = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        store = new Warehouse("Основной");
        store2 = new Warehouse("Второй");
        lot = new Lot("Партия");
        serialNumber = new SerialNumber("1", lot);
    }

    @Test
    @DisplayName("Приемка, отгрузка и повторный приход серийника обновляют остаток на каждом этапе")
    void simpleThingLifecycle() {
        GoodsAcceptance acceptance = goodsAcceptance(store, lot, serialNumber, rollMinutes(1));
        Shipment shipment = shipment(store, lot, serialNumber, rollMinutes(2));
        GoodsAcceptance returning = goodsAcceptance(store, lot, serialNumber, rollMinutes(3));

        thingStockManager.addSerialsToWarehouse(acceptance);
        thingStockManager.writeOffSerialsFromWarehouse(shipment);
        thingStockManager.addSerialsToWarehouse(returning);

        assertThat(firstStock(serialNumber))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(1, rollMinutes(1), rollMinutes(2));
        assertThat(stockAtMoment(serialNumber, rollMinutes(2)))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(0, rollMinutes(2), rollMinutes(3));
        assertThat(lastStock(serialNumber))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(1, rollMinutes(3), null);
    }

    @Test
    @DisplayName("Приемка, отгрузка и повторный приход в один момент оставляют серийник на складе")
    void simpleThingLifecycleWithSameMoment() {
        LocalDateTime moment = rollMinutes(1);
        thingStockManager.addSerialsToWarehouse(goodsAcceptance(store, lot, serialNumber, moment));
        thingStockManager.writeOffSerialsFromWarehouse(shipment(store, lot, serialNumber, moment));
        thingStockManager.addSerialsToWarehouse(goodsAcceptance(store, lot, serialNumber, moment));

        assertThat(firstStock(serialNumber))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(1, moment, moment);
        assertThat(lastStock(serialNumber))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(1, moment, null);
    }

    @Test
    @DisplayName("Отгрузка и повторный приход в один момент не обнуляют остаток до следующей отгрузки")
    void middleStocksWithSameMoment() {
        thingStockManager.addSerialsToWarehouse(goodsAcceptance(store, lot, serialNumber, rollMinutes(1)));
        Shipment shipment = shipment(store, lot, serialNumber, rollMinutes(2));
        GoodsAcceptance returning = goodsAcceptance(store, lot, serialNumber, rollMinutes(2));
        thingStockManager.writeOffSerialsFromWarehouse(shipment);
        thingStockManager.addSerialsToWarehouse(returning);
        thingStockManager.writeOffSerialsFromWarehouse(shipment(store, lot, serialNumber, rollMinutes(3)));

        assertThat(firstStock(serialNumber))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(1, rollMinutes(1), rollMinutes(2));
        assertThat(stockForDocument(serialNumber, shipment.getId()))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(1, rollMinutes(2), rollMinutes(2));
        assertThat(stockForDocument(serialNumber, returning.getId()))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(1, rollMinutes(2), rollMinutes(3));
        assertThat(lastStock(serialNumber))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(0, rollMinutes(3), null);
    }

    @Test
    @DisplayName("Перенос последней операции на самое раннее время пересчитывает остаток")
    void changeMomentFromLastToFirst() {
        thingStockManager.addSerialsToWarehouse(goodsAcceptance(store, lot, serialNumber, rollMinutes(1)));
        thingStockManager.writeOffSerialsFromWarehouse(shipment(store, lot, serialNumber, rollMinutes(2)));
        GoodsAcceptance returning = goodsAcceptance(store, lot, serialNumber, rollMinutes(3));
        thingStockManager.addSerialsToWarehouse(returning);

        returning.setMoment(rollMinutes(-1));
        thingStockManager.addSerialsToWarehouse(returning);

        assertThat(firstStock(serialNumber))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(1, rollMinutes(-1), rollMinutes(1));
        assertThat(stockAtMoment(serialNumber, rollMinutes(1)))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(2, rollMinutes(1), rollMinutes(2));
        assertThat(lastStock(serialNumber))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(1, rollMinutes(2), null);
    }

    @Test
    @DisplayName("Перенос первой приемки в конец цепочки оставляет отрицательный остаток до нового прихода")
    void changeMomentFromFirstToLast() {
        GoodsAcceptance acceptance = goodsAcceptance(store, lot, serialNumber, rollMinutes(1));
        thingStockManager.addSerialsToWarehouse(acceptance);
        thingStockManager.writeOffSerialsFromWarehouse(shipment(store, lot, serialNumber, rollMinutes(2)));
        thingStockManager.addSerialsToWarehouse(goodsAcceptance(store, lot, serialNumber, rollMinutes(3)));

        acceptance.setMoment(rollMinutes(4));
        thingStockManager.addSerialsToWarehouse(acceptance);

        assertThat(firstStock(serialNumber))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(-1, rollMinutes(2), rollMinutes(3));
        assertThat(stockAtMoment(serialNumber, rollMinutes(3)))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(0, rollMinutes(3), rollMinutes(4));
        assertThat(lastStock(serialNumber))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(1, rollMinutes(4), null);
    }

    @Test
    @DisplayName("Удаление первой приемки пересчитывает остаток серийника")
    void deleteFirstOperation() {
        GoodsAcceptance acceptance = goodsAcceptance(store, lot, serialNumber, rollMinutes(1));
        thingStockManager.addSerialsToWarehouse(acceptance);
        thingStockManager.writeOffSerialsFromWarehouse(shipment(store, lot, serialNumber, rollMinutes(2)));
        thingStockManager.addSerialsToWarehouse(goodsAcceptance(store, lot, serialNumber, rollMinutes(3)));

        thingStockManager.removeOperation(acceptance);

        assertThat(firstStock(serialNumber, store))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(-1, rollMinutes(2), rollMinutes(3));
        assertThat(lastStock(serialNumber))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(0, rollMinutes(3), null);
    }

    @Test
    @DisplayName("Смена склада приемки переносит остаток серийника")
    void changeTargetPlace() {
        GoodsAcceptance acceptance = goodsAcceptance(store, lot, serialNumber, rollMinutes(1));
        thingStockManager.addSerialsToWarehouse(acceptance);
        thingStockManager.writeOffSerialsFromWarehouse(shipment(store, lot, serialNumber, rollMinutes(2)));
        thingStockManager.addSerialsToWarehouse(goodsAcceptance(store, lot, serialNumber, rollMinutes(3)));

        acceptance.setTargetStore(store2);
        thingStockManager.addSerialsToWarehouse(acceptance);

        assertThat(firstStock(serialNumber, store))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(-1, rollMinutes(2), rollMinutes(3));
        assertThat(lastStock(serialNumber, store))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(0, rollMinutes(3), null);
        assertThat(lastStock(serialNumber, store2))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(1, rollMinutes(1), null);
    }

    @Test
    @DisplayName("Снятие проведения с приемки убирает серийник из остатка")
    void setOperationApplicable() {
        GoodsAcceptance acceptance = goodsAcceptance(store, lot, serialNumber, rollMinutes(1));
        thingStockManager.addSerialsToWarehouse(acceptance);
        thingStockManager.writeOffSerialsFromWarehouse(shipment(store, lot, serialNumber, rollMinutes(2)));
        thingStockManager.addSerialsToWarehouse(goodsAcceptance(store, lot, serialNumber, rollMinutes(3)));

        acceptance.setApplicable(false);
        thingStockManager.addSerialsToWarehouse(acceptance);

        assertThat(firstStock(serialNumber))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(-1, rollMinutes(2), rollMinutes(3));
        assertThat(lastStock(serialNumber))
                .extracting(Stock::getStock, Stock::getMomentFrom, Stock::getMomentTo)
                .containsExactly(0, rollMinutes(3), null);
    }

    @Test
    @DisplayName("Добавление серийника на склад создает остаток 1")
    void addSerialsToWarehouse() {
        thingStockManager.addSerialsToWarehouse(goodsAcceptance(store, lot, serialNumber, rollMinutes(1)));

        assertThat(lastStock(serialNumber, store))
                .extracting(Stock::getStock, Stock::getDelta, Stock::getMomentTo)
                .containsExactly(1, 1, null);
    }

    @Test
    @DisplayName("Повторное добавление серийника отклоняется, остаток не меняется")
    void addSerialsRejectsWhenAlreadyInStock() {
        thingStockManager.addSerialsToWarehouse(goodsAcceptance(store, lot, serialNumber, rollMinutes(1)));

        assertThatThrownBy(() -> thingStockManager.addSerialsToWarehouse(goodsAcceptance(store, lot, serialNumber, rollMinutes(2))))
                .isInstanceOf(ThingAlreadyInStockException.class)
                .extracting(ex -> ((ThingAlreadyInStockException) ex).getSerials())
                .isEqualTo(java.util.Set.of("1"));
        assertThat(lastStock(serialNumber, store).getStock()).isEqualTo(1);
    }

    @Test
    @DisplayName("Списание серийника со склада обнуляет остаток")
    void writeOffSerialsFromWarehouse() {
        thingStockManager.addSerialsToWarehouse(goodsAcceptance(store, lot, serialNumber, rollMinutes(1)));
        thingStockManager.writeOffSerialsFromWarehouse(shipment(store, lot, serialNumber, rollMinutes(2)));

        assertThat(lastStock(serialNumber, store))
                .extracting(Stock::getStock, Stock::getDelta)
                .containsExactly(0, -1);
    }

    @Test
    @DisplayName("Списание серийника, которого нет на складе, отклоняется")
    void writeOffRejectsWhenNotInStock() {
        assertThatThrownBy(() -> thingStockManager.writeOffSerialsFromWarehouse(shipment(store, lot, serialNumber, rollMinutes(1))))
                .isInstanceOf(ThingNotInStockException.class)
                .extracting(ex -> ((ThingNotInStockException) ex).getSerials())
                .isEqualTo(java.util.Set.of("1"));
        assertThat(stockRepository.count()).isZero();
    }

    @Test
    @DisplayName("Перемещение списывает серийник со склада-отправителя и добавляет на склад-получатель")
    void shiftTransfersSerialBetweenWarehouses() {
        thingStockManager.addSerialsToWarehouse(goodsAcceptance(store, lot, serialNumber, rollMinutes(1)));
        Shift shift = shift(store, store2, lot, serialNumber, rollMinutes(2));
        thingStockManager.writeOffSerialsFromWarehouse(shift);

        assertThat(lastStock(serialNumber, store).getStock()).isZero();
        assertThat(lastStock(serialNumber, store2))
                .extracting(Stock::getStock, Stock::getDelta, Stock::getMomentFrom)
                .containsExactly(1, 1, rollMinutes(2));
    }

    @Test
    @DisplayName("Приемка того же серийника на другой склад отклоняется: он уже числится на остатке")
    void addSerialsRejectsWhenAlreadyInStockOnAnotherWarehouse() {
        thingStockManager.addSerialsToWarehouse(goodsAcceptance(store, lot, serialNumber, rollMinutes(1)));

        assertThatThrownBy(() -> thingStockManager.addSerialsToWarehouse(goodsAcceptance(store2, lot, serialNumber, rollMinutes(2))))
                .isInstanceOf(ThingAlreadyInStockException.class);
        assertThat(lastStock(serialNumber, store).getStock()).isEqualTo(1);
        assertThat(stocks(serialNumber, store2)).isEmpty();
    }

    @Test
    @DisplayName("Техоперация списывает материал и приходует выпущенный серийник")
    void machiningConsumesMaterialAndProducesSerial() {
        thingStockManager.addSerialsToWarehouse(goodsAcceptance(store, lot, serialNumber, rollMinutes(1)));
        SerialNumber product = new SerialNumber("2", lot);
        Machining machining = machining(store, store, lot, serialNumber, product, rollMinutes(2));

        thingStockManager.writeOffSerialsFromWarehouse(machining);

        assertThat(lastStock(serialNumber, store).getStock()).isZero();
        assertThat(lastStock(product, store).getStock()).isEqualTo(1);
    }

    @Test
    @DisplayName("Выполнение этапа производства списывает материал и приходует продукцию")
    void makeConsumesMaterialAndProducesSerial() {
        thingStockManager.addSerialsToWarehouse(goodsAcceptance(store, lot, serialNumber, rollMinutes(1)));
        SerialNumber product = new SerialNumber("2", lot);
        Make make = make(store, store2, lot, serialNumber, product, rollMinutes(2));

        thingStockManager.addSerialsToWarehouse(make);

        assertThat(lastStock(serialNumber, store).getStock()).isZero();
        assertThat(lastStock(product, store2).getStock()).isEqualTo(1);
    }

    @Test
    @DisplayName("Пакетная проверка двух приемок одного серийника на одну дату отклоняет обе")
    void checkThingStocksRejectsDuplicateSerialsInOneBatch() {
        GoodsAcceptance first = goodsAcceptance(store, lot, serialNumber, rollMinutes(1));
        GoodsAcceptance second = goodsAcceptance(store, lot, serialNumber, rollMinutes(1));
        persist(first, second);

        assertThatThrownBy(() -> thingStockManager.checkThingStocks(List.of(first, second)))
                .isInstanceOf(ThingAlreadyInStockException.class)
                .extracting(ex -> ((ThingAlreadyInStockException) ex).getSerials())
                .isEqualTo(java.util.Set.of("1"));
    }

    @Test
    @DisplayName("Пакетная проверка отгрузки серийника, которого нет на складе, отклоняет документ")
    void checkThingStocksRejectsShipmentWithoutStock() {
        Shipment shipment = shipment(store, lot, serialNumber, rollMinutes(1));
        persist(shipment);

        assertThatThrownBy(() -> thingStockManager.checkThingStocks(List.of(shipment)))
                .isInstanceOf(ThingNotInStockException.class)
                .extracting(ex -> ((ThingNotInStockException) ex).getSerials())
                .isEqualTo(java.util.Set.of("1"));
    }

    @Test
    @DisplayName("Пакетная проверка перемещения серийника, которого нет на складе-отправителе, отклоняет документ")
    void checkThingStocksRejectsShiftWithoutStock() {
        Shift shift = shift(store, store2, lot, serialNumber, rollMinutes(1));
        persist(shift);

        assertThatThrownBy(() -> thingStockManager.checkThingStocks(List.of(shift)))
                .isInstanceOf(ThingNotInStockException.class)
                .extracting(ex -> ((ThingNotInStockException) ex).getSerials())
                .isEqualTo(java.util.Set.of("1"));
    }

    @Test
    @DisplayName("Пакетная проверка техоперации без материала на складе отклоняет документ")
    void checkThingStocksRejectsMachiningWithoutMaterial() {
        Machining machining = machining(store, store, lot, serialNumber, new SerialNumber("2", lot), rollMinutes(1));
        persist(machining);

        assertThatThrownBy(() -> thingStockManager.checkThingStocks(List.of(machining)))
                .isInstanceOf(ThingNotInStockException.class)
                .extracting(ex -> ((ThingNotInStockException) ex).getSerials())
                .isEqualTo(java.util.Set.of("1"));
    }

    @Test
    @DisplayName("Пакетная проверка выполнения этапа без материала на складе отклоняет документ")
    void checkThingStocksRejectsMakeWithoutMaterial() {
        Make make = make(store, store2, lot, serialNumber, new SerialNumber("2", lot), rollMinutes(1));
        persist(make);

        assertThatThrownBy(() -> thingStockManager.checkThingStocks(List.of(make)))
                .isInstanceOf(ThingNotInStockException.class)
                .extracting(ex -> ((ThingNotInStockException) ex).getSerials())
                .isEqualTo(java.util.Set.of("1"));
    }

    private GoodsAcceptance goodsAcceptance(Warehouse warehouse, Lot goods, SerialNumber serial, LocalDateTime moment) {
        GoodsAcceptance document = new GoodsAcceptance();
        document.setName("Приемка");
        document.setMoment(moment);
        document.setApplicable(true);
        document.setTargetStore(warehouse);
        document.addMotion(goods, serial, PositionType.POSITION);
        return document;
    }

    private Shipment shipment(Warehouse warehouse, Lot goods, SerialNumber serial, LocalDateTime moment) {
        Shipment document = new Shipment();
        document.setName("Отгрузка");
        document.setMoment(moment);
        document.setApplicable(true);
        document.setSourceStore(warehouse);
        document.addMotion(goods, serial, PositionType.POSITION);
        return document;
    }

    private Shift shift(Warehouse source, Warehouse target, Lot goods, SerialNumber serial, LocalDateTime moment) {
        Shift document = new Shift();
        document.setName("Перемещение");
        document.setMoment(moment);
        document.setApplicable(true);
        document.setSourceStore(source);
        document.setTargetStore(target);
        document.addMotion(goods, serial, PositionType.POSITION);
        return document;
    }

    private Machining machining(Warehouse source, Warehouse target, Lot goods, SerialNumber material, SerialNumber product, LocalDateTime moment) {
        Machining document = new Machining();
        document.setName("Техоперация");
        document.setMoment(moment);
        document.setApplicable(true);
        document.setSourceStore(source);
        document.setTargetStore(target);
        document.addMotion(goods, material, PositionType.MATERIAL);
        document.addMotion(goods, product, PositionType.RESULT);
        return document;
    }

    private Make make(Warehouse source, Warehouse target, Lot goods, SerialNumber material, SerialNumber product, LocalDateTime moment) {
        Make document = new Make();
        document.setName("Этап");
        document.setMoment(moment);
        document.setApplicable(true);
        document.setSourceStore(source);
        document.setTargetStore(target);
        document.addMotion(goods, material, PositionType.MATERIAL);
        document.addMotion(goods, product, PositionType.RESULT);
        return document;
    }

    private void persist(DocumentsWithPositions... documents) {
        transactionTemplate.executeWithoutResult(status -> {
            for (DocumentsWithPositions document : documents) {
                entityManager.persist(document);
            }
        });
    }

    private LocalDateTime rollMinutes(long minutes) {
        return startMoment.plusMinutes(minutes);
    }

    private Stock firstStock(SerialNumber serial) {
        return stocks(serial).stream().min(stockOrder()).orElseThrow();
    }

    private Stock lastStock(SerialNumber serial) {
        return stocks(serial).stream().max(stockOrder()).orElseThrow();
    }

    private Stock firstStock(SerialNumber serial, Warehouse warehouse) {
        return stocks(serial, warehouse).stream().min(stockOrder()).orElseThrow();
    }

    private Stock lastStock(SerialNumber serial, Warehouse warehouse) {
        return stocks(serial, warehouse).stream().max(stockOrder()).orElseThrow();
    }

    private Stock stockAtMoment(SerialNumber serial, LocalDateTime moment) {
        return stocks(serial).stream()
                .filter(stock -> !stock.getMomentFrom().isAfter(moment))
                .max(Comparator.comparing(Stock::getMomentFrom))
                .orElseThrow();
    }

    private Stock stockForDocument(SerialNumber serial, UUID documentId) {
        return stocks(serial).stream()
                .filter(stock -> stock.getDocument().getId().equals(documentId))
                .findFirst()
                .orElseThrow();
    }

    private List<Stock> stocks(SerialNumber serial) {
        return stockRepository.findBySerialNumber_IdAndLot_Id(serial.getId(), serial.getLot().getId());
    }

    private List<Stock> stocks(SerialNumber serial, Warehouse warehouse) {
        return stocks(serial).stream()
                .filter(stock -> stock.getPlace().getId().equals(warehouse.getId()))
                .toList();
    }

    private Comparator<Stock> stockOrder() {
        return Comparator.comparing(Stock::getMomentFrom)
                .thenComparing(Stock::getMomentTo, Comparator.nullsLast(Comparator.naturalOrder()));
    }
}
