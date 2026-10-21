package ru.moysklad.intern.camunda.step;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.camunda.ProcessContext;
import ru.moysklad.intern.dto.kafka.StockChangePositionDto;
import ru.moysklad.intern.entity.Product;
import ru.moysklad.intern.entity.aggregate.AggregateStock;
import ru.moysklad.intern.entity.aggregate.Price;
import ru.moysklad.intern.entity.aggregate.StockMovement;
import ru.moysklad.intern.entity.aggregate.StockMovementOperationType;
import ru.moysklad.intern.entity.operation.DocumentsType;
import ru.moysklad.intern.entity.stock.Lot;
import ru.moysklad.intern.entity.warehouse.Warehouse;
import ru.moysklad.intern.repo.AggregateStockRepository;
import ru.moysklad.intern.repo.PriceRepository;
import ru.moysklad.intern.repo.StockMovementRepository;
import ru.moysklad.intern.service.aggregate.PriceService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Атомарное изменение AggregateStock и Price.
 *
 * <p><b>TODO (студент):</b> реализовать {@link #execute(ProcessContext)}.
 * Одна транзакция, защита от lost update между инстансами, запрет отрицательного остатка,
 * пересчёт totalCost, откат обоих изменений при ошибке.
 */
@Component
public class StockAndPriceUpdateStep {

    private final AggregateStockRepository aggregateStockRepository;
    private final PriceRepository priceRepository;
    private final PriceService priceService;
    private final StockMovementRepository stockMovementRepository;
    private final EntityManager entityManager;

    public StockAndPriceUpdateStep(
            AggregateStockRepository aggregateStockRepository,
            PriceRepository priceRepository,
            PriceService priceService,
            StockMovementRepository stockMovementRepository,
            EntityManager entityManager
    ) {
        this.aggregateStockRepository = aggregateStockRepository;
        this.priceRepository = priceRepository;
        this.priceService = priceService;
        this.stockMovementRepository = stockMovementRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public void execute(ProcessContext context) {
        DocumentsType docType = context.documentType();
        if (docType == null) {
            context.fail("INVALID_DOCUMENT", "Не указан тип документа");
            return;
        }

        List<StockChangePositionDto> positions = context.positions();
        if (positions == null || positions.isEmpty()) {
            context.fail("INVALID_DOCUMENT", "Позиции документа отсутствуют");
            return;
        }

        // дельты и новые цены по ключам остатков
        Map<StockKey, BigDecimal> netDeltas = new HashMap<>();
        Map<StockKey, BigDecimal> incomingPrices = new HashMap<>();

        for (StockChangePositionDto pos : positions) {
            BigDecimal qty = pos.quantity();
            if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            switch (docType) {
                case GOODS_ACCEPTANCE -> {
                    StockKey key = new StockKey(pos.productId(), pos.storeId(), pos.consignmentId());
                    netDeltas.merge(key, qty, BigDecimal::add);
                    if (pos.unitPrice() != null) {
                        incomingPrices.put(key, pos.unitPrice());
                    }
                }
                case SHIPMENT -> {
                    StockKey key = new StockKey(pos.productId(), pos.storeId(), pos.consignmentId());
                    netDeltas.merge(key, qty.negate(), BigDecimal::add);
                }
                case SHIFT -> {
                    StockKey srcKey = new StockKey(pos.productId(), pos.storeId(), pos.consignmentId());
                    netDeltas.merge(srcKey, qty.negate(), BigDecimal::add);

                    if (pos.destinationStoreId() != null) {
                        StockKey dstKey = new StockKey(pos.productId(), pos.destinationStoreId(), pos.consignmentId());
                        netDeltas.merge(dstKey, qty, BigDecimal::add);
                    }
                }
                case MACHINING, MAKE -> {
                    StockKey srcKey = new StockKey(pos.productId(), pos.storeId(), pos.consignmentId());
                    netDeltas.merge(srcKey, qty.negate(), BigDecimal::add);

                    if (pos.destinationStoreId() != null) {
                        StockKey dstKey = new StockKey(pos.productId(), pos.destinationStoreId(), pos.consignmentId());
                        netDeltas.merge(dstKey, qty, BigDecimal::add);
                    }
                }
            }
        }

        // ключи для детерминированного порядка блокировок
        List<StockKey> sortedKeys = new ArrayList<>(netDeltas.keySet());
        sortedKeys.sort(Comparator
                .comparing(StockKey::productId)
                .thenComparing(StockKey::storeId)
                .thenComparing(StockKey::consignmentId));

        Map<StockKey, AggregateStock> lockedStocks = new HashMap<>();

        // захват блокировок
        for (StockKey key : sortedKeys) {
            BigDecimal delta = netDeltas.get(key);
            AggregateStock stock = aggregateStockRepository
                    .findForUpdate(key.productId(), key.storeId(), key.consignmentId())
                    .orElse(null);

            if (delta.compareTo(BigDecimal.ZERO) < 0) {
                BigDecimal requiredDeduction = delta.abs();
                if (stock == null || stock.getQuantity().compareTo(requiredDeduction) < 0) {
                    BigDecimal available = stock == null ? BigDecimal.ZERO : stock.getQuantity();
                    BigDecimal missing = requiredDeduction.subtract(available);
                    context.fail(
                            "INSUFFICIENT_STOCK",
                            String.format("Недостаточно остатка под блокировкой. Доступно: %s, требуется: %s", available, requiredDeduction),
                            missing,
                            null
                    );
                    return;
                }

                if (!priceRepository.existsByStock_Id(stock.getId())) {
                    context.fail("PRICE_NOT_FOUND", "Не задана себестоимость для списываемого товара: " + key);
                    return;
                }
            } else if (delta.compareTo(BigDecimal.ZERO) > 0) {
                if (stock == null) {
                    stock = new AggregateStock(
                            entityManager.getReference(Product.class, key.productId()),
                            entityManager.getReference(Warehouse.class, key.storeId()),
                            entityManager.getReference(Lot.class, key.consignmentId()),
                            BigDecimal.ZERO
                    );
                    stock = aggregateStockRepository.saveAndFlush(stock);
                }
            }

            if (stock != null) {
                lockedStocks.put(key, stock);
            }
        }

        // внесение изменений
        for (StockKey key : sortedKeys) {
            BigDecimal delta = netDeltas.get(key);
            if (delta.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            AggregateStock stock = lockedStocks.get(key);
            BigDecimal newQuantity = stock.getQuantity().add(delta);
            if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
                context.fail("INSUFFICIENT_STOCK", "Остаток не может быть отрицательным: " + key, newQuantity.abs(), null);
                return;
            }

            stock.setQuantity(newQuantity);
            aggregateStockRepository.save(stock);

            // новая цена
            BigDecimal unitPrice = incomingPrices.get(key);
            if (unitPrice != null) {
                priceService.setUnitPrice(stock.getId(), unitPrice);
            } else {
                if (priceRepository.existsByStock_Id(stock.getId())) {
                    priceService.recalculateTotalCost(stock.getId());
                } else if (docType == DocumentsType.SHIFT) {
                    Price sourcePrice = findSourcePriceForShift(positions, key);
                    if (sourcePrice != null && sourcePrice.getUnitPrice() != null) {
                        priceService.setUnitPrice(stock.getId(), sourcePrice.getUnitPrice());
                    } else {
                        context.fail("PRICE_NOT_FOUND", "Не найдена цена для перемещаемого товара: " + key);
                        return;
                    }
                } else {
                    context.fail("PRICE_NOT_FOUND", "Не найдена цена для товара: " + key);
                    return;
                }
            }

            // фиксируем в истории движений
            StockMovement movement = new StockMovement();
            movement.setDocumentId(context.documentId());
            movement.setProduct(stock.getProduct());
            movement.setStore(stock.getStore());
            movement.setConsignment(stock.getConsignment());
            movement.setQuantityDelta(delta);
            movement.setMoment(context.moment());
            movement.setOperationType(toMovementOperationType(docType));
            stockMovementRepository.save(movement);
        }
    }

    private Price findSourcePriceForShift(List<StockChangePositionDto> positions, StockKey destKey) {
        for (StockChangePositionDto pos : positions) {
            if (pos.productId().equals(destKey.productId())
                    && pos.consignmentId().equals(destKey.consignmentId())
                    && destKey.storeId().equals(pos.destinationStoreId())) {
                AggregateStock sourceStock = aggregateStockRepository
                        .findByProduct_IdAndStore_IdAndConsignment_Id(pos.productId(), pos.storeId(), pos.consignmentId())
                        .orElse(null);
                if (sourceStock != null) {
                    return priceRepository.findByStock_Id(sourceStock.getId()).orElse(null);
                }
            }
        }
        return null;
    }

    private StockMovementOperationType toMovementOperationType(DocumentsType docType) {
        return switch (docType) {
            case GOODS_ACCEPTANCE -> StockMovementOperationType.GOODS_ACCEPTANCE;
            case SHIPMENT -> StockMovementOperationType.SHIPMENT;
            case SHIFT -> StockMovementOperationType.SHIFT;
            case MACHINING -> StockMovementOperationType.MACHINING;
            case MAKE -> StockMovementOperationType.MAKE;
        };
    }

    private record StockKey(UUID productId, UUID storeId, UUID consignmentId) {
    }
}
