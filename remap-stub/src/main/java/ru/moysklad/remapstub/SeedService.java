package ru.moysklad.remapstub;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SeedService implements ApplicationRunner {

    private final RemapEngine engine;
    private final ObjectMapper mapper;
    private final StubProperties properties;

    public SeedService(RemapEngine engine, ObjectMapper mapper, StubProperties properties) {
        this.engine = engine;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        reload();
    }

    public void reload() {
        engine.reset(this::load);
    }

    private void load() {
        save("organization", organization());
        save("store", store(SeedIds.STORE, "Основной склад"));
        save("store", store(SeedIds.STORE_SECOND, "Второй склад"));
        save("counterparty", counterparty());
        save("productfolder", folder());
        save("pricetype", priceType());
        save("currency", currency());
        save("product", product(SeedIds.PRODUCT, "Футболка", "TSHIRT", false));
        save("product", product(SeedIds.PRODUCT_ZERO, "Нет в наличии", "EMPTY", false));
        save("variant", variant());
        save("saleschannel", channel());
        save("state", state(SeedIds.STATE_NEW, "Новый", "Regular", "customerorder"));
        save("state", state(SeedIds.STATE_CANCELLED, "Отменён", "Unsuccessful", "customerorder"));
        engine.putStock(SeedIds.PRODUCT, SeedIds.STORE, new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO);
        engine.putStock(SeedIds.PRODUCT, SeedIds.STORE_SECOND, new BigDecimal("3"), BigDecimal.ZERO, BigDecimal.ZERO);
        engine.putStock(SeedIds.PRODUCT_ZERO, SeedIds.STORE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        engine.putStock(SeedIds.VARIANT, SeedIds.STORE, new BigDecimal("4"), BigDecimal.ZERO, BigDecimal.ZERO);
    }

    private void save(String type, ObjectNode entity) {
        engine.saveRaw(type, entity);
    }

    private ObjectNode organization() {
        ObjectNode node = base(SeedIds.ORGANIZATION, "ООО Учебный магазин");
        node.put("legalTitle", "ООО Учебный магазин");
        return node;
    }

    private ObjectNode store(String id, String name) {
        return base(id, name);
    }

    private ObjectNode counterparty() {
        ObjectNode node = base(SeedIds.COUNTERPARTY, "Покупатель");
        node.put("email", "buyer@example.com");
        node.put("phone", "+79990000000");
        return node;
    }

    private ObjectNode folder() {
        ObjectNode node = base(SeedIds.PRODUCT_FOLDER, "Каталог");
        node.put("pathName", "");
        return node;
    }

    private ObjectNode priceType() {
        ObjectNode node = base(SeedIds.PRICE_TYPE, "Цена продажи");
        node.put("externalCode", "sale");
        return node;
    }

    private ObjectNode currency() {
        ObjectNode node = base(SeedIds.CURRENCY, "руб");
        node.put("isoCode", "RUB");
        return node;
    }

    private ObjectNode product(String id, String name, String article, boolean archived) {
        ObjectNode node = base(id, name);
        node.put("article", article);
        node.put("code", article);
        node.put("archived", archived);
        node.put("description", name);
        node.set("productFolder", ref(SeedIds.PRODUCT_FOLDER, "productfolder"));
        ArrayNode prices = mapper.createArrayNode();
        ObjectNode price = mapper.createObjectNode();
        price.put("value", 150000);
        price.set("currency", ref(SeedIds.CURRENCY, "currency"));
        price.set("priceType", ref(SeedIds.PRICE_TYPE, "pricetype"));
        prices.add(price);
        node.set("salePrices", prices);
        ArrayNode barcodes = mapper.createArrayNode();
        ObjectNode barcode = mapper.createObjectNode();
        barcode.put("ean13", "2000000000015");
        barcodes.add(barcode);
        node.set("barcodes", barcodes);
        ArrayNode images = mapper.createArrayNode();
        ObjectNode image = mapper.createObjectNode();
        image.put("filename", "shirt.png");
        image.put("title", name);
        image.put("size", 68);
        ObjectNode imageMeta = mapper.createObjectNode();
        imageMeta.put("href", properties.publicBaseUrl() + "/api/remap/1.2/download/" + id);
        imageMeta.put("downloadHref", properties.publicBaseUrl() + "/api/remap/1.2/download/" + id);
        imageMeta.put("type", "image");
        imageMeta.put("mediaType", "application/json");
        image.set("meta", imageMeta);
        images.add(image);
        node.set("images", images);
        return node;
    }

    private ObjectNode variant() {
        ObjectNode node = base(SeedIds.VARIANT, "Футболка (Красный)");
        node.put("article", "TSHIRT-RED");
        node.put("archived", false);
        node.set("product", ref(SeedIds.PRODUCT, "product"));
        ArrayNode characteristics = mapper.createArrayNode();
        ObjectNode color = mapper.createObjectNode();
        color.put("id", "cccccccc-cccc-cccc-cccc-ccccccccccc1");
        color.put("name", "Цвет");
        color.put("value", "Красный");
        characteristics.add(color);
        node.set("characteristics", characteristics);
        ArrayNode prices = mapper.createArrayNode();
        ObjectNode price = mapper.createObjectNode();
        price.put("value", 160000);
        price.set("currency", ref(SeedIds.CURRENCY, "currency"));
        price.set("priceType", ref(SeedIds.PRICE_TYPE, "pricetype"));
        prices.add(price);
        node.set("salePrices", prices);
        return node;
    }

    private ObjectNode channel() {
        ObjectNode node = base(SeedIds.SALES_CHANNEL, "Интернет-магазин");
        node.put("type", "ECOMMERCE");
        node.put("archived", false);
        return node;
    }

    private ObjectNode state(String id, String name, String stateType, String entityType) {
        ObjectNode node = base(id, name);
        node.put("stateType", stateType);
        node.put("entityType", entityType);
        node.put("color", 10667543);
        return node;
    }

    private ObjectNode base(String id, String name) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", id);
        node.put("accountId", SeedIds.ACCOUNT);
        node.put("name", name);
        node.put("externalCode", id);
        node.put("updated", "2026-11-26 12:00:00");
        node.put("archived", false);
        return node;
    }

    private ObjectNode ref(String id, String type) {
        ObjectNode ref = mapper.createObjectNode();
        ObjectNode meta = mapper.createObjectNode();
        meta.put("href", properties.publicBaseUrl() + "/api/remap/1.2/entity/" + type + "/" + id);
        meta.put("type", type);
        meta.put("mediaType", "application/json");
        ref.set("meta", meta);
        return ref;
    }
}
