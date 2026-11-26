package ru.moysklad.concurrency.pricing;

import org.springframework.stereotype.Service;
import ru.moysklad.concurrency.domain.DiscountRule;
import ru.moysklad.concurrency.domain.Price;
import ru.moysklad.concurrency.domain.PricePolicy;
import ru.moysklad.concurrency.support.SyncPoint;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class PriceService {

    private final SyncPoint syncPoint;
    private final ConcurrentHashMap<String, Price> prices = new ConcurrentHashMap<>();
    private volatile PricePolicy published = initialPolicy();

    public PriceService() {
        this(SyncPoint.NOOP);
    }

    public PriceService(SyncPoint syncPoint) {
        this.syncPoint = syncPoint;
    }

    public Price setSalePrice(String productId, BigDecimal salePrice, BigDecimal discountPercent) {
        if (salePrice.signum() < 0) {
            throw new IllegalArgumentException("Цена не может быть отрицательной");
        }
        if (discountPercent.signum() < 0) {
            throw new IllegalArgumentException("Скидка не может быть отрицательной");
        }
        Price price = new Price(productId, salePrice, discountPercent);
        prices.put(productId, price);
        return price;
    }

    public Price price(String productId) {
        Price price = prices.get(productId);
        if (price == null) {
            throw new IllegalArgumentException("Нет цены для " + productId);
        }
        return price;
    }

    /**
     * Публикует правила скидок и минимальную наценку.
     * Потребитель, увидевший новую политику, всегда наблюдает полностью
     * заполненные правила и наценку.
     */
    public void updatePolicy(List<DiscountRule> rules, BigDecimal margin) {
        if (margin.signum() < 0) {
            throw new IllegalArgumentException("Наценка не может быть отрицательной");
        }
        PricePolicy policy = new PricePolicy();
        policy.initialize(rules, margin);
        published = policy;
        syncPoint.signal("policy-published");
        syncPoint.awaitSignal("policy-observed", 2, TimeUnit.SECONDS);
    }

    public PricePolicy currentPolicy() {
        return published;
    }

    private static PricePolicy initialPolicy() {
        PricePolicy policy = new PricePolicy();
        policy.initialize(List.of(), BigDecimal.ZERO);
        return policy;
    }
}
