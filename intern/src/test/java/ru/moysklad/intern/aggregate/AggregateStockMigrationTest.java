package ru.moysklad.intern.aggregate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.TestcontainersConfiguration;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.aggregate.AggregateStock;
import ru.moysklad.intern.entity.aggregate.Price;
import ru.moysklad.intern.entity.aggregate.StockMovement;
import ru.moysklad.intern.entity.aggregate.StockMovementOperationType;
import ru.moysklad.intern.entity.processing.OutboxEvent;
import ru.moysklad.intern.entity.processing.OutboxEventStatus;
import ru.moysklad.intern.entity.processing.ProcessingCommandType;
import ru.moysklad.intern.entity.processing.ProcessingRequest;
import ru.moysklad.intern.entity.processing.ProcessingRequestStatus;
import ru.moysklad.intern.entity.processing.ProcessingResult;
import ru.moysklad.intern.entity.processing.ProcessingResultStatus;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.warehouse.Warehouse;
import ru.moysklad.intern.repo.AggregateStockRepository;
import ru.moysklad.intern.repo.LotRepository;
import ru.moysklad.intern.repo.OutboxEventRepository;
import ru.moysklad.intern.repo.PriceRepository;
import ru.moysklad.intern.repo.ProcessingRequestRepository;
import ru.moysklad.intern.repo.ProcessingResultRepository;
import ru.moysklad.intern.repo.ProductRepository;
import ru.moysklad.intern.repo.StockMovementRepository;
import ru.moysklad.intern.repo.WarehouseRepository;
import ru.moysklad.intern.service.aggregate.PriceService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AggregateStockMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private AggregateStockRepository aggregateStockRepository;

    @Autowired
    private PriceRepository priceRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private ProcessingRequestRepository processingRequestRepository;

    @Autowired
    private ProcessingResultRepository processingResultRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private PriceService priceService;

    @Test
    void liquibaseCreatesAggregateAndProcessingTables() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                        select table_name
                        from information_schema.tables
                        where table_schema = 'public'
                          and table_name in (
                            'aggregate_stock', 'price', 'stock_movement',
                            'processing_request', 'processing_result', 'outbox_event'
                          )
                        order by table_name
                        """,
                String.class
        );

        assertThat(tables).containsExactly(
                "aggregate_stock",
                "outbox_event",
                "price",
                "processing_request",
                "processing_result",
                "stock_movement"
        );
    }

    @Test
    void aggregateStockUniqueConstraintAndForeignKeysExist() {
        Integer uniqueCount = jdbcTemplate.queryForObject(
                """
                        select count(*)
                        from information_schema.table_constraints
                        where table_name = 'aggregate_stock'
                          and constraint_type = 'UNIQUE'
                          and constraint_name = 'uq_aggregate_stock_product_store_consignment'
                        """,
                Integer.class
        );
        assertThat(uniqueCount).isEqualTo(1);

        List<Map<String, Object>> fks = jdbcTemplate.queryForList(
                """
                        select constraint_name
                        from information_schema.table_constraints
                        where table_name = 'aggregate_stock'
                          and constraint_type = 'FOREIGN KEY'
                        order by constraint_name
                        """
        );
        assertThat(fks).extracting(row -> row.get("constraint_name"))
                .contains(
                        "fk_aggregate_stock_consignment",
                        "fk_aggregate_stock_product",
                        "fk_aggregate_stock_store"
                );
    }

    @Test
    @Transactional
    void persistsAggregateStockAndPriceViaPriceService() {
        Product product = productRepository.save(newProduct("Товар-1"));
        Warehouse store = warehouseRepository.save(new Warehouse("Склад-1"));
        Lot lot = lotRepository.save(new Lot("Партия-1"));

        AggregateStock stock = aggregateStockRepository.save(
                new AggregateStock(product, store, lot, new BigDecimal("10.000000"))
        );

        Price price = priceService.setUnitPrice(stock.getId(), new BigDecimal("12.50"), "ART-1");

        assertThat(price.getId()).isNotNull();
        assertThat(price.getUnitPrice()).isEqualByComparingTo("12.500000");
        assertThat(price.getTotalCost()).isEqualByComparingTo("125.000000");
        assertThat(priceRepository.findByStock_Id(stock.getId())).isPresent();

        stock.setQuantity(new BigDecimal("8.000000"));
        aggregateStockRepository.saveAndFlush(stock);
        Price recalculated = priceService.recalculateTotalCost(stock.getId());
        assertThat(recalculated.getTotalCost()).isEqualByComparingTo("100.000000");
    }

    @Test
    @Transactional
    void priceServiceRejectsNegativeAndMissingPrice() {
        Product product = productRepository.save(newProduct("Товар-2"));
        Warehouse store = warehouseRepository.save(new Warehouse("Склад-2"));
        Lot lot = lotRepository.save(new Lot("Партия-2"));
        AggregateStock stock = aggregateStockRepository.save(
                new AggregateStock(product, store, lot, BigDecimal.TEN)
        );

        assertThatThrownBy(() -> priceService.setUnitPrice(stock.getId(), new BigDecimal("-1")))
                .isInstanceOf(ru.moysklad.intern.exception.aggregate.InvalidPriceException.class);

        assertThatThrownBy(() -> priceService.getRequiredPrice(stock.getId()))
                .isInstanceOf(ru.moysklad.intern.exception.aggregate.PriceNotFoundException.class);
    }

    @Test
    @Transactional
    void persistsStockMovementAndProcessingEntities() {
        Product product = productRepository.save(newProduct("Товар-3"));
        Warehouse store = warehouseRepository.save(new Warehouse("Склад-3"));
        Lot lot = lotRepository.save(new Lot("Партия-3"));

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setStore(store);
        movement.setConsignment(lot);
        movement.setQuantityDelta(new BigDecimal("5"));
        movement.setMoment(LocalDateTime.of(2026, 10, 14, 12, 0));
        movement.setOperationType(StockMovementOperationType.GOODS_ACCEPTANCE);
        movement.setDocumentId(UUID.randomUUID());
        stockMovementRepository.save(movement);

        BigDecimal sum = stockMovementRepository.sumQuantityDeltaUntil(
                product.getId(), store.getId(), lot.getId(), LocalDateTime.of(2026, 10, 14, 23, 59)
        );
        assertThat(sum).isEqualByComparingTo("5");

        UUID requestId = UUID.randomUUID();
        ProcessingRequest request = new ProcessingRequest();
        request.setRequestId(requestId);
        request.setCommandType(ProcessingCommandType.STOCK_CHANGE);
        request.setStatus(ProcessingRequestStatus.PENDING);
        request.setCorrelationId(UUID.randomUUID());
        request.setPayload("{\"ok\":true}");
        processingRequestRepository.save(request);

        ProcessingResult result = new ProcessingResult();
        result.setRequestId(requestId);
        result.setStatus(ProcessingResultStatus.SUCCESS);
        processingResultRepository.save(result);

        OutboxEvent event = new OutboxEvent();
        event.setTopic("processing.results");
        event.setEventKey(requestId.toString());
        event.setPayload("{\"status\":\"SUCCESS\"}");
        event.setStatus(OutboxEventStatus.PENDING);
        outboxEventRepository.save(event);

        assertThat(processingRequestRepository.findByRequestId(requestId)).isPresent();
        assertThat(processingResultRepository.findByRequestId(requestId)).isPresent();
        assertThat(outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxEventStatus.PENDING)).hasSize(1);
    }

    private static Product newProduct(String name) {
        Product product = new Product();
        product.setName(name);
        return product;
    }
}
