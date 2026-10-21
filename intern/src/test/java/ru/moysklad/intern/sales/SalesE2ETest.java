package ru.moysklad.intern.sales;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.TestcontainersConfiguration;
import ru.moysklad.intern.camunda.step.SalesRequestClaimStep;
import ru.moysklad.intern.dto.kafka.SalesCommand;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.aggregate.AggregateStock;
import ru.moysklad.intern.entity.aggregate.Price;
import ru.moysklad.intern.entity.processing.OutboxEvent;
import ru.moysklad.intern.entity.processing.ProcessingRequest;
import ru.moysklad.intern.entity.processing.ProcessingRequestStatus;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.warehouse.Warehouse;
import ru.moysklad.intern.repo.AggregateStockRepository;
import ru.moysklad.intern.repo.LotRepository;
import ru.moysklad.intern.repo.OutboxEventRepository;
import ru.moysklad.intern.repo.PriceRepository;
import ru.moysklad.intern.repo.ProcessingRequestRepository;
import ru.moysklad.intern.repo.ProductRepository;
import ru.moysklad.intern.repo.WarehouseRepository;
import ru.moysklad.intern.service.aggregate.PriceService;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class SalesE2ETest {

    @Autowired
    private SalesRequestService salesRequestService;

    @Autowired
    private SalesKafkaConsumer salesKafkaConsumer;

    @Autowired
    private SalesRequestClaimStep salesRequestClaimStep;

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
    private PriceService priceService;

    @Autowired
    private ProcessingRequestRepository processingRequestRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private Product testProduct;
    private Warehouse testStore;
    private Lot testLot;
    private AggregateStock testStock;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setName("E2E Товар-" + UUID.randomUUID());
        testProduct.setPrice(new BigDecimal("100.00"));
        testProduct = productRepository.save(testProduct);

        testStore = warehouseRepository.save(new Warehouse("E2E Склад-" + UUID.randomUUID()));
        testLot = lotRepository.save(new Lot("E2E Партия-" + UUID.randomUUID()));

        testStock = aggregateStockRepository.save(
                new AggregateStock(testProduct, testStore, testLot, new BigDecimal("10.000000"))
        );
        priceService.setUnitPrice(testStock.getId(), new BigDecimal("100.000000"), "ART-E2E");
    }

    @Test
    @DisplayName("Успешное проведение реализации: списание остатка, пересчет себестоимости и статус COMPLETED")
    void successfulSaleFlowUpdatesStockAndRecordsSuccess() {
        SalesCommand command = new SalesCommand(
                UUID.randomUUID(),
                testProduct.getId(),
                testStore.getId(),
                new BigDecimal("3"),
                new BigDecimal("500.00"),
                LocalDateTime.now(),
                null
        );

        UUID requestId = salesRequestService.submit(command);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(processingRequestRepository.findByRequestId(requestId)).isPresent()
        );

        salesRequestClaimStep.claimAndStart();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            ProcessingRequest request = processingRequestRepository.findByRequestId(requestId).orElseThrow();
            assertThat(request.getStatus()).isEqualTo(ProcessingRequestStatus.COMPLETED);

            AggregateStock updatedStock = aggregateStockRepository.findById(testStock.getId()).orElseThrow();
            assertThat(updatedStock.getQuantity()).isEqualByComparingTo("7.000000");

            Price updatedPrice = priceRepository.findByStock_Id(testStock.getId()).orElseThrow();
            assertThat(updatedPrice.getTotalCost()).isEqualByComparingTo("700.000000");
        });
    }

    @Test
    @DisplayName("Отказ при нехватке товара на складе: статус FAILED и неизменность остатков")
    void failsWhenStockIsInsufficient() {
        SalesCommand command = new SalesCommand(
                UUID.randomUUID(),
                testProduct.getId(),
                testStore.getId(),
                new BigDecimal("20"), // на складе только 10
                new BigDecimal("5000.00"),
                LocalDateTime.now(),
                null
        );

        UUID requestId = salesRequestService.submit(command);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(processingRequestRepository.findByRequestId(requestId)).isPresent()
        );

        salesRequestClaimStep.claimAndStart();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            ProcessingRequest request = processingRequestRepository.findByRequestId(requestId).orElseThrow();
            assertThat(request.getStatus()).isEqualTo(ProcessingRequestStatus.FAILED);

            AggregateStock stock = aggregateStockRepository.findById(testStock.getId()).orElseThrow();
            assertThat(stock.getQuantity()).isEqualByComparingTo("10.000000");
        });
    }

    @Test
    @DisplayName("Отказ при нехватке доступных средств у покупателя: статус FAILED и неизменность остатков")
    void failsWhenFundsAreInsufficient() {
        SalesCommand command = new SalesCommand(
                UUID.randomUUID(),
                testProduct.getId(),
                testStore.getId(),
                new BigDecimal("3"), // требуется 3 * 100 = 300
                new BigDecimal("150.00"), // доступно только 150
                LocalDateTime.now(),
                null
        );

        UUID requestId = salesRequestService.submit(command);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(processingRequestRepository.findByRequestId(requestId)).isPresent()
        );

        salesRequestClaimStep.claimAndStart();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            ProcessingRequest request = processingRequestRepository.findByRequestId(requestId).orElseThrow();
            assertThat(request.getStatus()).isEqualTo(ProcessingRequestStatus.FAILED);

            AggregateStock stock = aggregateStockRepository.findById(testStock.getId()).orElseThrow();
            assertThat(stock.getQuantity()).isEqualByComparingTo("10.000000");
        });
    }

    @Test
    @DisplayName("Идемпотентность Kafka Consumer: игнорирование повторных сообщений с одинаковым requestId")
    void idempotentConsumerIgnoresDuplicateCommand() {
        SalesCommand command = new SalesCommand(
                UUID.randomUUID(),
                testProduct.getId(),
                testStore.getId(),
                new BigDecimal("1"),
                new BigDecimal("200.00"),
                LocalDateTime.now(),
                null
        );

        salesKafkaConsumer.consume(command);
        assertThat(processingRequestRepository.findByRequestId(command.requestId())).isPresent();

        // повторная доставка не должна приводить к ошибкам или дублям
        salesKafkaConsumer.consume(command);
        assertThat(processingRequestRepository.findByRequestId(command.requestId())).isPresent();
    }
}
