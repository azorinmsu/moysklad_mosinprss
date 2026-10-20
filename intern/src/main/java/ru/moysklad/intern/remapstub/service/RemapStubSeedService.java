package ru.moysklad.intern.remapstub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.remapstub.config.RemapStubProperties;
import ru.moysklad.intern.remapstub.entity.StubCustomerOrder;
import ru.moysklad.intern.remapstub.entity.StubDemand;
import ru.moysklad.intern.remapstub.entity.StubDemandPosition;
import ru.moysklad.intern.remapstub.entity.StubOrderPosition;
import ru.moysklad.intern.remapstub.entity.StubProduct;
import ru.moysklad.intern.remapstub.entity.StubStock;
import ru.moysklad.intern.remapstub.repo.StubCustomerOrderRepository;
import ru.moysklad.intern.remapstub.repo.StubDemandPositionRepository;
import ru.moysklad.intern.remapstub.repo.StubDemandRepository;
import ru.moysklad.intern.remapstub.repo.StubFaultRuleRepository;
import ru.moysklad.intern.remapstub.repo.StubOrderPositionRepository;
import ru.moysklad.intern.remapstub.repo.StubProductRepository;
import ru.moysklad.intern.remapstub.repo.StubStockRepository;
import ru.moysklad.intern.remapstub.seed.RemapStubIds;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Order(100)
@ConditionalOnProperty(prefix = "app.remap-stub", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RemapStubSeedService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RemapStubSeedService.class);

    private final RemapStubProperties properties;
    private final StubProductRepository productRepository;
    private final StubStockRepository stockRepository;
    private final StubCustomerOrderRepository orderRepository;
    private final StubOrderPositionRepository positionRepository;
    private final StubDemandRepository demandRepository;
    private final StubDemandPositionRepository demandPositionRepository;
    private final StubFaultRuleRepository faultRuleRepository;

    public RemapStubSeedService(
            RemapStubProperties properties,
            StubProductRepository productRepository,
            StubStockRepository stockRepository,
            StubCustomerOrderRepository orderRepository,
            StubOrderPositionRepository positionRepository,
            StubDemandRepository demandRepository,
            StubDemandPositionRepository demandPositionRepository,
            StubFaultRuleRepository faultRuleRepository
    ) {
        this.properties = properties;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
        this.orderRepository = orderRepository;
        this.positionRepository = positionRepository;
        this.demandRepository = demandRepository;
        this.demandPositionRepository = demandPositionRepository;
        this.faultRuleRepository = faultRuleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isSeedOnStartup()) {
            return;
        }
        if (productRepository.count() > 0) {
            log.info("Remap stub seed skipped: data already present");
            return;
        }
        resetToSeed();
        log.info("Remap stub seed loaded");
    }

    @Transactional
    public void resetToSeed() {
        faultRuleRepository.deleteAllInBatch();
        demandPositionRepository.deleteAllInBatch();
        demandRepository.deleteAllInBatch();
        positionRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        stockRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();

        saveProduct(RemapStubIds.PRODUCT_SUFFICIENT, "Товар с достаточным остатком", "SUFFICIENT", "100 шт на складе");
        saveProduct(RemapStubIds.PRODUCT_ZERO, "Товар с нулевым остатком", "ZERO", "нет на складе");
        saveProduct(RemapStubIds.PRODUCT_PARTIAL, "Товар с частичным резервом", "PARTIAL", "20 шт, из них 12 в резерве");

        saveStock(RemapStubIds.PRODUCT_SUFFICIENT, bd(100), bd(0));
        saveStock(RemapStubIds.PRODUCT_ZERO, bd(0), bd(0));
        saveStock(RemapStubIds.PRODUCT_PARTIAL, bd(20), bd(12));

        LocalDateTime now = LocalDateTime.now().withNano(0);

        saveOrder(RemapStubIds.ORDER_SINGLE, "Заказ с одной позицией", "достаточно остатков", now);
        saveOrderPosition(RemapStubIds.ORDER_SINGLE_POS, RemapStubIds.ORDER_SINGLE, RemapStubIds.PRODUCT_SUFFICIENT, bd(4), bd(0), 15000);

        saveOrder(RemapStubIds.ORDER_MULTI, "Заказ с несколькими позициями", "две позиции", now);
        saveOrderPosition(RemapStubIds.ORDER_MULTI_POS_1, RemapStubIds.ORDER_MULTI, RemapStubIds.PRODUCT_SUFFICIENT, bd(3), bd(0), 15000);
        saveOrderPosition(RemapStubIds.ORDER_MULTI_POS_2, RemapStubIds.ORDER_MULTI, RemapStubIds.PRODUCT_PARTIAL, bd(2), bd(0), 20000);

        saveOrder(RemapStubIds.ORDER_INSUFFICIENT, "Заказ с недостаточным остатком", "требует 5 при available=0", now);
        saveOrderPosition(RemapStubIds.ORDER_INSUFFICIENT_POS, RemapStubIds.ORDER_INSUFFICIENT, RemapStubIds.PRODUCT_ZERO, bd(5), bd(0), 10000);

        saveOrder(RemapStubIds.ORDER_WITH_RESERVE, "Заказ с существующим резервом", "резерв уже установлен", now);
        saveOrderPosition(RemapStubIds.ORDER_WITH_RESERVE_POS, RemapStubIds.ORDER_WITH_RESERVE, RemapStubIds.PRODUCT_PARTIAL, bd(5), bd(5), 20000);
        // PRODUCT_PARTIAL already has reserve=12 in stock (includes this order's 5 + other 7 fictional)

        saveOrder(RemapStubIds.ORDER_WITH_DEMAND, "Заказ с существующей отгрузкой", "есть demand", now);
        saveOrderPosition(RemapStubIds.ORDER_WITH_DEMAND_POS, RemapStubIds.ORDER_WITH_DEMAND, RemapStubIds.PRODUCT_SUFFICIENT, bd(2), bd(2), 15000);
        // account for this reserve in stock
        StubStock sufficient = stockRepository.findById(RemapStubIds.PRODUCT_SUFFICIENT).orElseThrow();
        sufficient.setReserve(sufficient.getReserve().add(bd(2)));
        stockRepository.save(sufficient);

        StubDemand demand = new StubDemand();
        demand.setId(RemapStubIds.DEMAND_EXISTING);
        demand.setName("00001");
        demand.setDescription("Черновик отгрузки для ORDER_WITH_DEMAND");
        demand.setApplicable(false);
        demand.setMoment(now);
        demand.setOrganizationId(RemapStubIds.ORGANIZATION_ID);
        demand.setAgentId(RemapStubIds.COUNTERPARTY_ID);
        demand.setStoreId(RemapStubIds.STORE_ID);
        demand.setCustomerOrderId(RemapStubIds.ORDER_WITH_DEMAND);
        demandRepository.save(demand);

        StubDemandPosition demandPosition = new StubDemandPosition();
        demandPosition.setId(RemapStubIds.DEMAND_EXISTING_POS);
        demandPosition.setDemandId(RemapStubIds.DEMAND_EXISTING);
        demandPosition.setProductId(RemapStubIds.PRODUCT_SUFFICIENT);
        demandPosition.setQuantity(bd(2));
        demandPosition.setPrice(15000);
        demandPositionRepository.save(demandPosition);
    }

    private void saveProduct(java.util.UUID id, String name, String code, String description) {
        StubProduct product = new StubProduct();
        product.setId(id);
        product.setName(name);
        product.setCode(code);
        product.setDescription(description);
        product.setArchived(false);
        productRepository.save(product);
    }

    private void saveStock(java.util.UUID productId, BigDecimal quantity, BigDecimal reserve) {
        StubStock stock = new StubStock();
        stock.setProductId(productId);
        stock.setStoreId(RemapStubIds.STORE_ID);
        stock.setQuantity(quantity);
        stock.setReserve(reserve);
        stock.setInTransit(BigDecimal.ZERO);
        stockRepository.save(stock);
    }

    private void saveOrder(java.util.UUID id, String name, String description, LocalDateTime moment) {
        StubCustomerOrder order = new StubCustomerOrder();
        order.setId(id);
        order.setName(name);
        order.setDescription(description);
        order.setApplicable(true);
        order.setMoment(moment);
        order.setOrganizationId(RemapStubIds.ORGANIZATION_ID);
        order.setAgentId(RemapStubIds.COUNTERPARTY_ID);
        order.setStoreId(RemapStubIds.STORE_ID);
        orderRepository.save(order);
    }

    private void saveOrderPosition(
            java.util.UUID id,
            java.util.UUID orderId,
            java.util.UUID productId,
            BigDecimal quantity,
            BigDecimal reserve,
            long price
    ) {
        StubOrderPosition position = new StubOrderPosition();
        position.setId(id);
        position.setOrderId(orderId);
        position.setProductId(productId);
        position.setQuantity(quantity);
        position.setReserve(reserve);
        position.setPrice(price);
        position.setShipped(BigDecimal.ZERO);
        positionRepository.save(position);
    }

    private static BigDecimal bd(int value) {
        return BigDecimal.valueOf(value);
    }
}
