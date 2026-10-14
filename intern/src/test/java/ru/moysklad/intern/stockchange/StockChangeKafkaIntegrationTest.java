package ru.moysklad.intern.stockchange;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import ru.moysklad.intern.TestcontainersConfiguration;
import ru.moysklad.intern.dto.kafka.StockChangeCommand;
import ru.moysklad.intern.dto.kafka.StockChangePositionDto;
import ru.moysklad.intern.dto.stockchange.StockChangeRequest;
import ru.moysklad.intern.entity.operation.DocumentsType;
import ru.moysklad.intern.entity.processing.ProcessingRequestStatus;
import ru.moysklad.intern.repo.ProcessingRequestRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Import({TestcontainersConfiguration.class, StockChangeKafkaIntegrationTest.CaptureConfig.class})
@SpringBootTest
class StockChangeKafkaIntegrationTest {

    @Autowired
    private StockChangeRequestService stockChangeRequestService;

    @Autowired
    private ProcessingRequestRepository processingRequestRepository;

    @Autowired
    private CapturingStockChangeProcessLauncher capturingLauncher;

    @Test
    void submitPublishesCommandAndConsumerStartsProcessing() {
        UUID documentId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID consignmentId = UUID.randomUUID();

        StockChangeRequest request = new StockChangeRequest(
                documentId,
                DocumentsType.GOODS_ACCEPTANCE,
                LocalDateTime.of(2026, 10, 14, 15, 0),
                List.of(new StockChangePositionDto(
                        productId,
                        storeId,
                        null,
                        consignmentId,
                        new BigDecimal("5"),
                        List.of("SN-1"),
                        new BigDecimal("10.00")
                )),
                null
        );

        UUID requestId = stockChangeRequestService.submit(request);

        assertThat(processingRequestRepository.findByRequestId(requestId)).isPresent();

        await().atMost(Duration.ofSeconds(45)).untilAsserted(() -> {
            assertThat(processingRequestRepository.findByRequestId(requestId).orElseThrow().getStatus())
                    .isEqualTo(ProcessingRequestStatus.PROCESSING);

            StockChangeCommand launched = capturingLauncher.lastCommand();
            assertThat(launched).isNotNull();
            assertThat(launched.requestId()).isEqualTo(requestId);
            assertThat(launched.documentId()).isEqualTo(documentId);
            assertThat(launched.documentType()).isEqualTo(DocumentsType.GOODS_ACCEPTANCE);
            assertThat(launched.positions()).hasSize(1);
            assertThat(launched.positions().getFirst().productId()).isEqualTo(productId);
            assertThat(launched.positions().getFirst().quantity()).isEqualByComparingTo("5");
            assertThat(capturingLauncher.commands()).hasSize(1);
        });
    }

    @TestConfiguration
    static class CaptureConfig {

        @Bean
        @Primary
        CapturingStockChangeProcessLauncher capturingStockChangeProcessLauncher() {
            return new CapturingStockChangeProcessLauncher();
        }
    }

    static class CapturingStockChangeProcessLauncher implements StockChangeProcessLauncher {

        private final CopyOnWriteArrayList<StockChangeCommand> commands = new CopyOnWriteArrayList<>();
        private final AtomicReference<StockChangeCommand> last = new AtomicReference<>();

        @Override
        public void launch(StockChangeCommand command) {
            commands.add(command);
            last.set(command);
        }

        List<StockChangeCommand> commands() {
            return List.copyOf(commands);
        }

        StockChangeCommand lastCommand() {
            return last.get();
        }
    }
}
