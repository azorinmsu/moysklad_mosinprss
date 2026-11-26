package ru.moysklad.remapstub;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Физический остаток, резерв и ожидание по паре ассортимент+склад.
 * Пересчёт в заглушке синхронный: отчёт видит эффект документа сразу после ответа.
 */
public final class StockLedger {

    public record Bucket(BigDecimal stock, BigDecimal reserve, BigDecimal inTransit, Instant changedAt) {
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void clear() {
        buckets.clear();
    }

    public void setAbsolute(String assortmentId, String storeId, BigDecimal stock, BigDecimal reserve, BigDecimal inTransit) {
        buckets.put(key(assortmentId, storeId), new Bucket(stock, reserve, inTransit, Instant.now()));
    }

    public void add(String assortmentId, String storeId, BigDecimal stockDelta, BigDecimal reserveDelta, BigDecimal inTransitDelta) {
        String mapKey = key(assortmentId, storeId);
        Bucket current = buckets.getOrDefault(mapKey, new Bucket(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Instant.EPOCH));
        buckets.put(mapKey, new Bucket(
                current.stock.add(stockDelta),
                current.reserve.add(reserveDelta),
                current.inTransit.add(inTransitDelta),
                Instant.now()));
    }

    public Map<String, Bucket> snapshot() {
        return Map.copyOf(buckets);
    }

    public static String key(String assortmentId, String storeId) {
        return assortmentId + "|" + (storeId == null ? "" : storeId);
    }

    public static BigDecimal freeStock(Bucket bucket) {
        return bucket.stock.subtract(bucket.reserve);
    }

    public static BigDecimal available(Bucket bucket) {
        return bucket.stock.subtract(bucket.reserve).add(bucket.inTransit);
    }
}
