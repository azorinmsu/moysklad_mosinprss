package ru.moysklad.intern.remapstub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.remapstub.entity.StubProduct;
import ru.moysklad.intern.remapstub.entity.StubStock;
import ru.moysklad.intern.remapstub.repo.StubProductRepository;
import ru.moysklad.intern.remapstub.repo.StubStockRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class RemapStockService {

    private final StubStockRepository stockRepository;
    private final StubProductRepository productRepository;
    private final RemapJsonMapper jsonMapper;
    private final RemapMetaFactory metaFactory;

    public RemapStockService(
            StubStockRepository stockRepository,
            StubProductRepository productRepository,
            RemapJsonMapper jsonMapper,
            RemapMetaFactory metaFactory
    ) {
        this.stockRepository = stockRepository;
        this.productRepository = productRepository;
        this.jsonMapper = jsonMapper;
        this.metaFactory = metaFactory;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reportAll() {
        List<StubStock> stocks = stockRepository.findAll();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (StubStock stock : stocks) {
            StubProduct product = productRepository.findById(stock.getProductId()).orElse(null);
            if (product == null || product.isArchived()) {
                continue;
            }
            rows.add(jsonMapper.stockReportRow(product, stock));
        }
        return metaFactory.listEntity("stock", "/report/stock/all", rows);
    }

    @Transactional
    public StubStock lockStock(UUID productId) {
        return stockRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> RemapStubException.notFound("Остаток не найден для товара: " + productId));
    }

    @Transactional
    public void applyReserveDelta(UUID productId, BigDecimal delta) {
        StubStock stock = lockStock(productId);
        BigDecimal newReserve = stock.getReserve().add(delta);
        if (newReserve.compareTo(BigDecimal.ZERO) < 0) {
            throw RemapStubException.badRequest("Резерв не может стать отрицательным для товара " + productId);
        }
        if (stock.getQuantity().subtract(newReserve).subtract(stock.getInTransit()).compareTo(BigDecimal.ZERO) < 0) {
            throw RemapStubException.conflict("Недостаточно доступного остатка для резервирования товара " + productId);
        }
        stock.setReserve(newReserve);
        stockRepository.save(stock);
    }

    @Transactional
    public void applyShip(UUID productId, BigDecimal quantity, BigDecimal reserveToRelease) {
        StubStock stock = lockStock(productId);
        BigDecimal newQuantity = stock.getQuantity().subtract(quantity);
        BigDecimal newReserve = stock.getReserve().subtract(reserveToRelease);
        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw RemapStubException.conflict("Недостаточно остатка для отгрузки товара " + productId);
        }
        if (newReserve.compareTo(BigDecimal.ZERO) < 0) {
            newReserve = BigDecimal.ZERO;
        }
        stock.setQuantity(newQuantity);
        stock.setReserve(newReserve);
        stockRepository.save(stock);
    }

    @Transactional
    public void restoreShip(UUID productId, BigDecimal quantity, BigDecimal reserveToRestore) {
        StubStock stock = lockStock(productId);
        stock.setQuantity(stock.getQuantity().add(quantity));
        stock.setReserve(stock.getReserve().add(reserveToRestore));
        stockRepository.save(stock);
    }
}
