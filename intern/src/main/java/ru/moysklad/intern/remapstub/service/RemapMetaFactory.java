package ru.moysklad.intern.remapstub.service;

import org.springframework.stereotype.Component;
import ru.moysklad.intern.remapstub.config.RemapStubProperties;
import ru.moysklad.intern.remapstub.seed.RemapStubIds;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class RemapMetaFactory {

    public static final DateTimeFormatter MOMENT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final RemapStubProperties properties;

    public RemapMetaFactory(RemapStubProperties properties) {
        this.properties = properties;
    }

    public String apiBase() {
        return properties.apiBase();
    }

    public Map<String, Object> entityMeta(String type, UUID id) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("href", apiBase() + "/entity/" + type + "/" + id);
        meta.put("metadataHref", apiBase() + "/entity/" + type + "/metadata");
        meta.put("type", type);
        meta.put("mediaType", "application/json");
        return meta;
    }

    public Map<String, Object> collectionMeta(String type, String pathSuffix, int size) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("href", apiBase() + pathSuffix);
        meta.put("type", type);
        meta.put("mediaType", "application/json");
        meta.put("size", size);
        meta.put("limit", 1000);
        meta.put("offset", 0);
        return meta;
    }

    public Map<String, Object> ref(String type, UUID id) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("meta", entityMeta(type, id));
        return ref;
    }

    public Map<String, Object> namedRef(String type, UUID id, String name) {
        Map<String, Object> ref = ref(type, id);
        ref.put("id", id.toString());
        ref.put("name", name);
        return ref;
    }

    public Map<String, Object> listEntity(String type, String pathSuffix, List<Map<String, Object>> rows) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("meta", collectionMeta(type, pathSuffix, rows.size()));
        body.put("rows", rows);
        return body;
    }

    public Map<String, Object> stockBlock(BigDecimal quantity, BigDecimal reserve, BigDecimal inTransit) {
        BigDecimal available = quantity.subtract(reserve).subtract(inTransit);
        Map<String, Object> stock = new LinkedHashMap<>();
        stock.put("quantity", quantity.doubleValue());
        stock.put("reserve", reserve.doubleValue());
        stock.put("intransit", inTransit.doubleValue());
        stock.put("available", available.doubleValue());
        return stock;
    }

    public String formatMoment(LocalDateTime moment) {
        return moment.format(MOMENT_FORMAT);
    }

    public String accountId() {
        return RemapStubIds.ACCOUNT_ID.toString();
    }
}
