package ru.moysklad.intern.service.aggregate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.moysklad.intern.entity.aggregate.AggregateStock;
import ru.moysklad.intern.entity.aggregate.Price;
import ru.moysklad.intern.exception.aggregate.InvalidPriceException;
import ru.moysklad.intern.exception.aggregate.PriceNotFoundException;
import ru.moysklad.intern.repo.AggregateStockRepository;
import ru.moysklad.intern.repo.PriceRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Управление себестоимостью отдельно от проведения документа.
 * {@code totalCost = quantity × unitPrice}.
 */
@Service
public class PriceService {

    private static final int MONEY_SCALE = 6;

    private final PriceRepository priceRepository;
    private final AggregateStockRepository aggregateStockRepository;

    public PriceService(PriceRepository priceRepository, AggregateStockRepository aggregateStockRepository) {
        this.priceRepository = priceRepository;
        this.aggregateStockRepository = aggregateStockRepository;
    }

    @Transactional(readOnly = true)
    public Price getRequiredPrice(UUID stockId) {
        return priceRepository.findByStock_Id(stockId)
                .orElseThrow(() -> new PriceNotFoundException(stockId));
    }

    @Transactional(readOnly = true)
    public Price getRequiredPrice(AggregateStock stock) {
        if (stock == null || stock.getId() == null) {
            throw new PriceNotFoundException("Остаток не задан");
        }
        return getRequiredPrice(stock.getId());
    }

    /**
     * Устанавливает или обновляет unitPrice для остатка и пересчитывает totalCost.
     */
    @Transactional
    public Price setUnitPrice(UUID stockId, BigDecimal unitPrice) {
        return setUnitPrice(stockId, unitPrice, null);
    }

    @Transactional
    public Price setUnitPrice(UUID stockId, BigDecimal unitPrice, String article) {
        validateUnitPrice(unitPrice);

        AggregateStock stock = aggregateStockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("AggregateStock не найден: " + stockId));

        Price price = priceRepository.findByStock_Id(stockId).orElseGet(Price::new);
        if (price.getId() == null) {
            price.setStock(stock);
            price.setProduct(stock.getProduct());
        }
        if (article != null) {
            price.setArticle(article);
        }
        price.setUnitPrice(normalize(unitPrice));
        price.setTotalCost(calculateTotalCost(stock.getQuantity(), price.getUnitPrice()));
        return priceRepository.save(price);
    }

    /**
     * Пересчитывает totalCost по текущему quantity остатка и сохранённому unitPrice.
     */
    @Transactional
    public Price recalculateTotalCost(UUID stockId) {
        AggregateStock stock = aggregateStockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("AggregateStock не найден: " + stockId));
        Price price = getRequiredPrice(stockId);
        price.setTotalCost(calculateTotalCost(stock.getQuantity(), price.getUnitPrice()));
        return priceRepository.save(price);
    }

    public BigDecimal calculateTotalCost(BigDecimal quantity, BigDecimal unitPrice) {
        if (quantity == null) {
            throw new InvalidPriceException("Количество не задано");
        }
        validateUnitPrice(unitPrice);
        return normalize(quantity.multiply(unitPrice));
    }

    private void validateUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null) {
            throw new InvalidPriceException("Цена не задана");
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidPriceException("Цена не может быть отрицательной");
        }
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
