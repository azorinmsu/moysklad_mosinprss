package ru.moysklad.remapstub;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class RemapEngine {

    private static final DateTimeFormatter MOMENT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Set<String> DOCUMENTS = Set.of("customerorder", "demand", "salesreturn", "paymentin", "paymentout");
    private static final Set<String> WITH_POSITIONS = Set.of("customerorder", "demand", "salesreturn");
    private static final Set<String> READ_ONLY = Set.of(
            "id", "meta", "accountId", "created", "updated", "payedSum", "sum", "shippedSum", "reservedSum", "stock");

    private final ObjectMapper mapper;
    private final StubProperties properties;
    private final StockLedger stock = new StockLedger();
    private final WebhookBus webhooks;
    private final Map<String, LinkedHashMap<String, ObjectNode>> entities = new LinkedHashMap<>();
    private final Map<String, String> syncIndex = new LinkedHashMap<>();
    private final List<ObjectNode> audit = new ArrayList<>();
    private long nameSeq;

    public RemapEngine(ObjectMapper mapper, StubProperties properties, WebhookBus webhooks) {
        this.mapper = mapper;
        this.properties = properties;
        this.webhooks = webhooks;
    }

    public StockLedger stock() {
        return stock;
    }

    public synchronized void reset(Runnable seed) {
        entities.clear();
        syncIndex.clear();
        audit.clear();
        stock.clear();
        nameSeq = 0;
        seed.run();
    }

    public synchronized ResponseEntity<JsonNode> dispatch(HttpServletRequest request, JsonNode body) {
        String rest = request.getRequestURI().substring("/api/remap/1.2/".length());
        String method = request.getMethod();
        Query query = Query.from(request);
        if (rest.equals("security/token") && method.equals("POST")) {
            ObjectNode token = mapper.createObjectNode();
            token.put("access_token", "stub-access-token");
            return ResponseEntity.ok(token);
        }
        if (rest.equals("context/employee") && method.equals("GET")) {
            return ResponseEntity.ok(employee());
        }
        if (rest.equals("context/companysettings/pricetype") && method.equals("GET")) {
            return ResponseEntity.ok(listOfType("pricetype"));
        }
        if (rest.equals("audit") && method.equals("GET")) {
            return ResponseEntity.ok(auditPage(query));
        }
        if (rest.equals("report/stock/all") && method.equals("GET")) {
            return ResponseEntity.ok(stockReport(query, false, false));
        }
        if (rest.equals("report/stock/bystore") && method.equals("GET")) {
            return ResponseEntity.ok(stockReport(query, true, false));
        }
        if (rest.equals("report/stock/all/current") && method.equals("GET")) {
            return ResponseEntity.ok(stockCurrent(query, false));
        }
        if (rest.equals("report/stock/bystore/current") && method.equals("GET")) {
            return ResponseEntity.ok(stockCurrent(query, true));
        }
        if (rest.equals("entity/assortment") && method.equals("GET")) {
            return ResponseEntity.ok(assortment(query));
        }
        if (!rest.startsWith("entity/")) {
            throw RemapException.notFound("Ресурс не входит в подмножество заглушки: " + rest);
        }
        return ResponseEntity.ok(entity(method, rest.substring("entity/".length()), query, body, request));
    }

    public synchronized ObjectNode putStock(String assortmentId, String storeId, BigDecimal onHand, BigDecimal reserve, BigDecimal inTransit) {
        stock.setAbsolute(assortmentId, storeId, onHand, reserve, inTransit);
        ObjectNode result = mapper.createObjectNode();
        result.put("assortmentId", assortmentId);
        result.put("storeId", storeId);
        result.put("stock", onHand);
        result.put("reserve", reserve);
        result.put("inTransit", inTransit);
        return result;
    }

    private JsonNode entity(String method, String path, Query query, JsonNode body, HttpServletRequest request) {
        String[] parts = path.split("/");
        String type = parts[0];
        if (parts.length == 1) {
            if (method.equals("GET")) {
                return page(type, query);
            }
            if (method.equals("POST")) {
                if (body != null && body.isArray()) {
                    ArrayNode created = mapper.createArrayNode();
                    for (JsonNode item : body) {
                        created.add(create(type, item, request));
                    }
                    return created;
                }
                return create(type, body, request);
            }
        }
        if (parts.length == 2 && parts[1].equals("metadata") && method.equals("GET")) {
            return metadata(type);
        }
        if (parts.length == 2 && parts[1].equals("new") && method.equals("PUT")) {
            return template(type, body);
        }
        if (parts.length == 2) {
            String id = parts[1];
            if (method.equals("GET")) {
                return present(type, required(type, id), query, true);
            }
            if (method.equals("PUT")) {
                return update(type, id, body, request);
            }
            if (method.equals("DELETE")) {
                delete(type, id, request);
                return mapper.createObjectNode();
            }
        }
        if (parts.length == 3 && parts[2].equals("trash") && method.equals("POST")) {
            return trash(type, parts[1], request);
        }
        if (parts.length == 3 && parts[2].equals("positions") && method.equals("GET")) {
            return positionPage(type, parts[1], query);
        }
        if (parts.length == 3 && parts[2].equals("positions") && method.equals("POST")) {
            return addPosition(type, parts[1], body, request);
        }
        if (parts.length == 4 && parts[2].equals("positions")) {
            if (method.equals("GET")) {
                return presentPosition(type, required(type, parts[1]), parts[3], query);
            }
            if (method.equals("PUT")) {
                return updatePosition(type, parts[1], parts[3], body, request);
            }
            if (method.equals("DELETE")) {
                deletePosition(type, parts[1], parts[3], request);
                return mapper.createObjectNode();
            }
        }
        throw RemapException.notFound("Ресурс не входит в подмножество заглушки: entity/" + path);
    }

    private ObjectNode create(String type, JsonNode body, HttpServletRequest request) {
        if (body == null || !body.isObject()) {
            throw RemapException.badRequest("Тело запроса должно быть JSON-объектом");
        }
        if (body.hasNonNull("syncId")) {
            String existingId = syncIndex.get(type + "|" + body.get("syncId").asText());
            if (existingId != null && entities.getOrDefault(type, new LinkedHashMap<>()).containsKey(existingId)) {
                return present(type, entities.get(type).get(existingId), Query.none(), true);
            }
        }
        if (DOCUMENTS.contains(type)) {
            requireRef(body, "organization");
            requireRef(body, "agent");
        }
        if (type.equals("variant")) {
            requireRef(body, "product");
        }
        ObjectNode stored = mapper.createObjectNode();
        String id = UUID.randomUUID().toString();
        stored.put("id", id);
        stored.put("accountId", SeedIds.ACCOUNT);
        String now = now();
        stored.put("created", now);
        stored.put("updated", now);
        mergeWritable(stored, body);
        if (!stored.hasNonNull("name") || stored.get("name").asText().isBlank()) {
            stored.put("name", String.format("%05d", ++nameSeq));
        }
        if (!stored.has("applicable") && DOCUMENTS.contains(type)) {
            stored.put("applicable", true);
        }
        if (!stored.has("archived")) {
            stored.put("archived", false);
        }
        if ((type.equals("webhook") || type.equals("webhookstock")) && !stored.has("enabled")) {
            stored.put("enabled", true);
        }
        if (WITH_POSITIONS.contains(type)) {
            stored.set("_positions", readPositions(body.get("positions"), null));
            validateReserves(stored);
        }
        if (type.equals("demand") && truth(stored, "applicable") && idFromMeta(stored.get("store")) == null) {
            throw RemapException.precondition("У проведённой отгрузки должен быть указан склад");
        }
        bucket(type).put(id, stored);
        if (stored.hasNonNull("syncId")) {
            syncIndex.put(type + "|" + stored.get("syncId").asText(), id);
        }
        try {
            applyCreateEffects(type, stored);
        } catch (RuntimeException exception) {
            bucket(type).remove(id);
            syncIndex.remove(type + "|" + stored.path("syncId").asText());
            throw exception;
        }
        rememberAudit(type, id, "CREATE");
        emit(type, id, "CREATE", request);
        return present(type, stored, Query.none(), true);
    }

    private ObjectNode update(String type, String id, JsonNode body, HttpServletRequest request) {
        ObjectNode current = required(type, id);
        ObjectNode previous = current.deepCopy();
        if (body != null && body.isObject()) {
            mergeWritable(current, body);
            if (WITH_POSITIONS.contains(type) && body.has("positions")) {
                current.set("_positions", readPositions(body.get("positions"), positionsOf(previous)));
                validateReserves(current);
            }
        }
        current.put("updated", now());
        if (type.equals("demand") && truth(current, "applicable") && idFromMeta(current.get("store")) == null) {
            throw RemapException.precondition("У проведённой отгрузки должен быть указан склад");
        }
        applyUpdateEffects(type, previous, current);
        rememberAudit(type, id, "UPDATE");
        emit(type, id, "UPDATE", request);
        return present(type, current, Query.none(), true);
    }

    private void delete(String type, String id, HttpServletRequest request) {
        ObjectNode current = required(type, id);
        if (type.equals("customerorder") && linkedDemand(id) != null) {
            throw RemapException.conflict("Заказ связан с отгрузкой и не может быть удалён");
        }
        if (type.equals("demand") && truth(current, "applicable")) {
            throw RemapException.conflict("Проведённую отгрузку нельзя удалить. Снимите проведение");
        }
        applyDeleteEffects(type, current);
        bucket(type).remove(id);
        rememberAudit(type, id, "DELETE");
        emit(type, id, "DELETE", request);
    }

    private ObjectNode trash(String type, String id, HttpServletRequest request) {
        ObjectNode current = required(type, id);
        if (type.equals("demand") && truth(current, "applicable")) {
            reverseDemand(current);
            current.put("applicable", false);
        }
        current.put("deleted", now());
        current.put("updated", now());
        bucket(type).remove(id);
        bucket(type + "-trash").put(id, current);
        rememberAudit(type, id, "DELETE");
        emit(type, id, "DELETE", request);
        return present(type, current, Query.none(), true);
    }

    private void applyCreateEffects(String type, ObjectNode stored) {
        switch (type) {
            case "customerorder" -> holdReserves(stored);
            case "demand" -> {
                if (truth(stored, "applicable")) {
                    forwardDemand(stored);
                }
                linkDemand(stored);
            }
            case "salesreturn" -> {
                if (truth(stored, "applicable")) {
                    forwardReturn(stored);
                }
            }
            case "paymentin", "paymentout" -> recomputePayments();
            default -> {
            }
        }
    }

    private void applyUpdateEffects(String type, ObjectNode previous, ObjectNode current) {
        switch (type) {
            case "customerorder" -> {
                releaseReserves(previous);
                holdReserves(current);
            }
            case "demand" -> {
                if (truth(previous, "applicable")) {
                    reverseDemand(previous);
                }
                if (truth(current, "applicable")) {
                    forwardDemand(current);
                }
            }
            case "salesreturn" -> {
                if (truth(previous, "applicable")) {
                    reverseReturn(previous);
                }
                if (truth(current, "applicable")) {
                    forwardReturn(current);
                }
            }
            case "paymentin", "paymentout" -> recomputePayments();
            default -> {
            }
        }
    }

    private void applyDeleteEffects(String type, ObjectNode current) {
        switch (type) {
            case "customerorder" -> releaseReserves(current);
            case "demand" -> {
                if (truth(current, "applicable")) {
                    reverseDemand(current);
                }
            }
            case "salesreturn" -> {
                if (truth(current, "applicable")) {
                    reverseReturn(current);
                }
            }
            case "paymentin", "paymentout" -> {
                bucket(type).remove(current.get("id").asText());
                recomputePayments();
            }
            default -> {
            }
        }
    }

    private void holdReserves(ObjectNode order) {
        if (!truth(order, "applicable")) {
            return;
        }
        for (JsonNode position : positionsOf(order)) {
            stock.add(assortmentId(position), storeId(order), BigDecimal.ZERO, decimal(position, "reserve"), BigDecimal.ZERO);
        }
    }

    private void releaseReserves(ObjectNode order) {
        if (!truth(order, "applicable")) {
            return;
        }
        for (JsonNode position : positionsOf(order)) {
            stock.add(assortmentId(position), storeId(order), BigDecimal.ZERO, decimal(position, "reserve").negate(), BigDecimal.ZERO);
        }
    }

    private void forwardDemand(ObjectNode demand) {
        String orderId = idFromMeta(demand.get("customerOrder"));
        ObjectNode order = orderId == null ? null : entities.getOrDefault("customerorder", new LinkedHashMap<>()).get(orderId);
        if (order != null) {
            Map<String, BigDecimal> needed = new LinkedHashMap<>();
            for (JsonNode position : positionsOf(demand)) {
                needed.merge(assortmentId(position), decimal(position, "quantity"), BigDecimal::add);
            }
            for (var entry : needed.entrySet()) {
                BigDecimal room = BigDecimal.ZERO;
                for (JsonNode position : positionsOf(order)) {
                    if (entry.getKey().equals(assortmentId(position))) {
                        room = room.add(decimal(position, "quantity").subtract(decimal(position, "shipped")));
                    }
                }
                if (entry.getValue().compareTo(room) > 0) {
                    throw RemapException.badRequest("Количество отгрузки превышает неотгруженный остаток заказа");
                }
            }
        }
        for (JsonNode node : positionsOf(demand)) {
            ObjectNode position = (ObjectNode) node;
            BigDecimal quantity = decimal(position, "quantity");
            BigDecimal released = BigDecimal.ZERO;
            if (order != null) {
                ObjectNode orderPosition = matchOrderPosition(order, assortmentId(position));
                if (orderPosition != null) {
                    BigDecimal room = decimal(orderPosition, "quantity").subtract(decimal(orderPosition, "shipped"));
                    if (quantity.compareTo(room) > 0) {
                        throw RemapException.badRequest("Количество отгрузки превышает неотгруженный остаток заказа");
                    }
                    released = decimal(orderPosition, "reserve").min(quantity);
                    orderPosition.put("reserve", decimal(orderPosition, "reserve").subtract(released));
                    orderPosition.put("shipped", decimal(orderPosition, "shipped").add(quantity));
                    stock.add(assortmentId(position), storeId(order), BigDecimal.ZERO, released.negate(), BigDecimal.ZERO);
                }
            }
            stock.add(assortmentId(position), storeId(demand), quantity.negate(), BigDecimal.ZERO, BigDecimal.ZERO);
            position.put("_appliedQty", quantity);
            position.put("_releasedReserve", released);
        }
    }

    private void reverseDemand(ObjectNode demand) {
        String orderId = idFromMeta(demand.get("customerOrder"));
        ObjectNode order = orderId == null ? null : entities.getOrDefault("customerorder", new LinkedHashMap<>()).get(orderId);
        for (JsonNode node : positionsOf(demand)) {
            ObjectNode position = (ObjectNode) node;
            BigDecimal quantity = position.has("_appliedQty") ? position.get("_appliedQty").decimalValue() : decimal(position, "quantity");
            BigDecimal released = position.has("_releasedReserve") ? position.get("_releasedReserve").decimalValue() : BigDecimal.ZERO;
            stock.add(assortmentId(position), storeId(demand), quantity, BigDecimal.ZERO, BigDecimal.ZERO);
            if (order != null) {
                ObjectNode orderPosition = matchOrderPosition(order, assortmentId(position));
                if (orderPosition != null) {
                    orderPosition.put("shipped", decimal(orderPosition, "shipped").subtract(quantity).max(BigDecimal.ZERO));
                    orderPosition.put("reserve", decimal(orderPosition, "reserve").add(released));
                    stock.add(assortmentId(position), storeId(order), BigDecimal.ZERO, released, BigDecimal.ZERO);
                }
            }
        }
    }

    private void forwardReturn(ObjectNode salesReturn) {
        for (JsonNode node : positionsOf(salesReturn)) {
            ObjectNode position = (ObjectNode) node;
            BigDecimal quantity = decimal(position, "quantity");
            stock.add(assortmentId(position), storeId(salesReturn), quantity, BigDecimal.ZERO, BigDecimal.ZERO);
            position.put("_appliedQty", quantity);
        }
    }

    private void reverseReturn(ObjectNode salesReturn) {
        for (JsonNode node : positionsOf(salesReturn)) {
            ObjectNode position = (ObjectNode) node;
            BigDecimal quantity = position.has("_appliedQty") ? position.get("_appliedQty").decimalValue() : decimal(position, "quantity");
            stock.add(assortmentId(position), storeId(salesReturn), quantity.negate(), BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    private void linkDemand(ObjectNode demand) {
        String orderId = idFromMeta(demand.get("customerOrder"));
        if (orderId == null) {
            return;
        }
        ObjectNode order = entities.getOrDefault("customerorder", new LinkedHashMap<>()).get(orderId);
        if (order == null) {
            return;
        }
        ArrayNode demands = order.has("demands") && order.get("demands").isArray()
                ? (ArrayNode) order.get("demands")
                : mapper.createArrayNode();
        demands.add(meta(demand.get("id").asText(), "demand"));
        order.set("demands", demands);
    }

    private ObjectNode linkedDemand(String orderId) {
        for (ObjectNode demand : bucket("demand").values()) {
            if (orderId.equals(idFromMeta(demand.get("customerOrder")))) {
                return demand;
            }
        }
        return null;
    }

    private void recomputePayments() {
        for (String type : List.of("customerorder", "demand", "salesreturn")) {
            for (ObjectNode document : bucket(type).values()) {
                document.put("payedSum", 0);
            }
        }
        for (String type : List.of("paymentin", "paymentout")) {
            for (ObjectNode payment : bucket(type).values()) {
                if (!truth(payment, "applicable")) {
                    continue;
                }
                JsonNode operations = payment.get("operations");
                if (operations == null || !operations.isArray()) {
                    continue;
                }
                for (JsonNode operation : operations) {
                    String targetType = operation.path("meta").path("type").asText(null);
                    String targetId = idFromMeta(operation);
                    if (targetType == null || targetId == null) {
                        continue;
                    }
                    ObjectNode target = bucket(targetType).get(targetId);
                    if (target == null) {
                        continue;
                    }
                    BigDecimal linked = operation.has("linkedSum")
                            ? operation.get("linkedSum").decimalValue()
                            : decimal(payment, "sum");
                    target.put("payedSum", decimal(target, "payedSum").add(linked));
                }
            }
        }
    }

    private ObjectNode template(String type, JsonNode body) {
        if (!type.equals("demand") && !type.equals("salesreturn")) {
            throw RemapException.notFound("Шаблон поддерживается для demand и salesreturn");
        }
        ObjectNode template = mapper.createObjectNode();
        template.put("applicable", true);
        template.put("vatEnabled", true);
        template.put("vatIncluded", true);
        template.put("payedSum", 0);
        template.put("moment", now());
        template.set("organization", meta(SeedIds.ORGANIZATION, "organization"));
        template.set("store", meta(SeedIds.STORE, "store"));
        ArrayNode rows = mapper.createArrayNode();
        if (body != null && body.has("customerOrder") && type.equals("demand")) {
            String orderId = idFromMeta(body.get("customerOrder"));
            ObjectNode order = required("customerorder", orderId);
            template.set("agent", order.get("agent").deepCopy());
            template.set("customerOrder", meta(orderId, "customerorder"));
            if (order.has("store")) {
                template.set("store", order.get("store").deepCopy());
            }
            for (JsonNode position : positionsOf(order)) {
                BigDecimal room = decimal(position, "quantity").subtract(decimal(position, "shipped"));
                if (room.signum() <= 0) {
                    continue;
                }
                ObjectNode row = mapper.createObjectNode();
                row.set("assortment", position.get("assortment").deepCopy());
                row.put("quantity", room);
                row.put("price", decimal(position, "price"));
                row.put("discount", decimal(position, "discount"));
                row.put("vat", position.path("vat").asInt(0));
                row.put("vatEnabled", position.path("vatEnabled").asBoolean(true));
                rows.add(row);
            }
        }
        if (body != null && body.has("demand") && type.equals("salesreturn")) {
            String demandId = idFromMeta(body.get("demand"));
            ObjectNode demand = required("demand", demandId);
            template.set("demand", meta(demandId, "demand"));
            template.set("agent", demand.get("agent").deepCopy());
            if (demand.has("store")) {
                template.set("store", demand.get("store").deepCopy());
            }
            for (JsonNode position : positionsOf(demand)) {
                ObjectNode row = mapper.createObjectNode();
                row.set("assortment", position.get("assortment").deepCopy());
                row.put("quantity", decimal(position, "quantity"));
                row.put("price", decimal(position, "price"));
                rows.add(row);
            }
        }
        ObjectNode positions = mapper.createObjectNode();
        positions.set("rows", rows);
        template.set("positions", positions);
        return template;
    }

    private ObjectNode addPosition(String type, String id, JsonNode body, HttpServletRequest request) {
        if (!WITH_POSITIONS.contains(type)) {
            throw RemapException.notFound("У сущности нет позиций");
        }
        ObjectNode previous = required(type, id).deepCopy();
        ObjectNode position = readOnePosition(body, null);
        positionsOf(required(type, id)).add(position);
        validateReserves(required(type, id));
        applyUpdateEffects(type, previous, required(type, id));
        required(type, id).put("updated", now());
        emit(type, id, "UPDATE", request);
        return presentPosition(type, required(type, id), position.get("id").asText(), Query.none());
    }

    private ObjectNode updatePosition(String type, String id, String positionId, JsonNode body, HttpServletRequest request) {
        ObjectNode document = required(type, id);
        ObjectNode previous = document.deepCopy();
        ObjectNode position = findPosition(document, positionId);
        if (body != null && body.isObject()) {
            if (body.has("quantity")) {
                position.put("quantity", body.get("quantity").decimalValue());
            }
            if (body.has("reserve") && type.equals("customerorder")) {
                position.put("reserve", body.get("reserve").decimalValue());
            }
            if (body.has("price")) {
                position.put("price", body.get("price").decimalValue());
            }
            if (body.has("assortment")) {
                position.set("assortment", body.get("assortment").deepCopy());
            }
        }
        if (decimal(position, "reserve").compareTo(decimal(position, "quantity")) > 0) {
            throw new RemapException(400, 17003, "Количество зарезервированного товара не может превышать количество позиции");
        }
        applyUpdateEffects(type, previous, document);
        document.put("updated", now());
        emit(type, id, "UPDATE", request);
        return presentPosition(type, document, positionId, Query.none());
    }

    private void deletePosition(String type, String id, String positionId, HttpServletRequest request) {
        ObjectNode document = required(type, id);
        ObjectNode previous = document.deepCopy();
        ArrayNode rows = positionsOf(document);
        for (int i = 0; i < rows.size(); i++) {
            if (positionId.equals(rows.get(i).path("id").asText())) {
                rows.remove(i);
                applyUpdateEffects(type, previous, document);
                document.put("updated", now());
                emit(type, id, "UPDATE", request);
                return;
            }
        }
        throw RemapException.notFound("Позиция не найдена");
    }

    private ArrayNode readPositions(JsonNode raw, ArrayNode previous) {
        if (raw == null || raw.isNull()) {
            return previous == null ? mapper.createArrayNode() : previous.deepCopy();
        }
        JsonNode rows = raw.isArray() ? raw : raw.get("rows");
        if (rows == null || !rows.isArray()) {
            return mapper.createArrayNode();
        }
        if (rows.size() > 1000) {
            throw new RemapException(413, 413, "В массиве больше 1000 элементов");
        }
        ArrayNode result = mapper.createArrayNode();
        for (JsonNode row : rows) {
            result.add(readOnePosition(row, previous));
        }
        return result;
    }

    private ObjectNode readOnePosition(JsonNode row, ArrayNode previous) {
        if (row == null || !row.isObject()) {
            throw RemapException.badRequest("Позиция должна быть объектом");
        }
        if (idFromMeta(row.get("assortment")) == null) {
            throw RemapException.precondition("У позиции не заполнен assortment");
        }
        if (!row.has("quantity")) {
            throw RemapException.precondition("У позиции не заполнено quantity");
        }
        String id = row.hasNonNull("id") ? row.get("id").asText() : UUID.randomUUID().toString();
        ObjectNode position = mapper.createObjectNode();
        position.put("id", id);
        position.put("accountId", SeedIds.ACCOUNT);
        position.set("assortment", row.get("assortment").deepCopy());
        position.put("quantity", row.get("quantity").decimalValue());
        position.put("price", row.has("price") ? row.get("price").decimalValue() : BigDecimal.ZERO);
        position.put("reserve", row.has("reserve") ? row.get("reserve").decimalValue() : BigDecimal.ZERO);
        position.put("discount", row.has("discount") ? row.get("discount").decimalValue() : BigDecimal.ZERO);
        position.put("vat", row.path("vat").asInt(0));
        position.put("vatEnabled", row.path("vatEnabled").asBoolean(true));
        BigDecimal shipped = BigDecimal.ZERO;
        if (previous != null) {
            for (JsonNode old : previous) {
                if (id.equals(old.path("id").asText())) {
                    shipped = decimal(old, "shipped");
                }
            }
        }
        position.put("shipped", shipped);
        return position;
    }

    private void validateReserves(ObjectNode document) {
        if (!document.path("id").asText("").isEmpty() && !"customerorder".equals(typeOfStored(document))) {
            // reserve rule applies to customer orders; other documents ignore reserve
        }
        for (JsonNode position : positionsOf(document)) {
            if (decimal(position, "reserve").compareTo(decimal(position, "quantity")) > 0) {
                throw new RemapException(400, 17003, "Количество зарезервированного товара не может превышать количество позиции");
            }
            if (decimal(position, "quantity").compareTo(decimal(position, "shipped")) < 0) {
                throw RemapException.badRequest("Количество позиции меньше уже отгруженного");
            }
        }
    }

    private String typeOfStored(ObjectNode document) {
        for (var entry : entities.entrySet()) {
            if (entry.getValue().containsValue(document)) {
                return entry.getKey();
            }
        }
        return "";
    }

    private ObjectNode matchOrderPosition(ObjectNode order, String assortmentId) {
        ObjectNode fallback = null;
        for (JsonNode node : positionsOf(order)) {
            ObjectNode position = (ObjectNode) node;
            if (!assortmentId.equals(assortmentId(position))) {
                continue;
            }
            if (decimal(position, "quantity").compareTo(decimal(position, "shipped")) > 0) {
                return position;
            }
            fallback = position;
        }
        return fallback;
    }

    private ObjectNode findPosition(ObjectNode document, String positionId) {
        for (JsonNode node : positionsOf(document)) {
            if (positionId.equals(node.path("id").asText())) {
                return (ObjectNode) node;
            }
        }
        throw RemapException.notFound("Позиция не найдена");
    }

    private ArrayNode positionsOf(ObjectNode document) {
        JsonNode value = document.get("_positions");
        if (value instanceof ArrayNode array) {
            return array;
        }
        ArrayNode created = mapper.createArrayNode();
        document.set("_positions", created);
        return created;
    }

    private ObjectNode present(String type, ObjectNode stored, Query query, boolean single) {
        ObjectNode out = mapper.createObjectNode();
        stored.properties().forEach(field -> {
            if (!field.getKey().startsWith("_")) {
                out.set(field.getKey(), field.getValue().deepCopy());
            }
        });
        out.set("meta", meta(stored.get("id").asText(), type));
        boolean allowExpand = single || query.limit <= 100;
        if (WITH_POSITIONS.contains(type)) {
            boolean expandPositions = allowExpand && query.expands("positions");
            boolean fieldsStock = allowExpand && "stock".equals(query.fields);
            ObjectNode positions = mapper.createObjectNode();
            ArrayNode rows = positionsOf(stored);
            positions.set("meta", collectionMeta(href(type, stored.get("id").asText()) + "/positions", type + "position", rows.size(), 1000, 0));
            if (expandPositions || fieldsStock) {
                ArrayNode presented = mapper.createArrayNode();
                for (JsonNode row : rows) {
                    presented.add(presentPositionNode(type, stored, (ObjectNode) row, fieldsStock));
                }
                positions.set("rows", presented);
            }
            out.set("positions", positions);
            fillMoney(out, rows);
        }
        if (allowExpand && query.expands("agent") && out.has("agent")) {
            expandRef(out, "agent");
        }
        if (allowExpand && query.expands("store") && out.has("store")) {
            expandRef(out, "store");
        }
        if (allowExpand && query.expands("organization") && out.has("organization")) {
            expandRef(out, "organization");
        }
        return out;
    }

    private ObjectNode presentPosition(String type, ObjectNode document, String positionId, Query query) {
        return presentPositionNode(type, document, findPosition(document, positionId), "stock".equals(query.fields));
    }

    private ObjectNode presentPositionNode(String type, ObjectNode document, ObjectNode position, boolean fieldsStock) {
        ObjectNode out = mapper.createObjectNode();
        position.properties().forEach(field -> {
            if (!field.getKey().startsWith("_")) {
                out.set(field.getKey(), field.getValue().deepCopy());
            }
        });
        if (!type.equals("customerorder")) {
            out.remove("reserve");
            out.remove("shipped");
        }
        if (fieldsStock) {
            StockLedger.Bucket bucket = stock.snapshot().getOrDefault(
                    StockLedger.key(assortmentId(position), storeId(document)),
                    new StockLedger.Bucket(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Instant.EPOCH));
            ObjectNode stockNode = mapper.createObjectNode();
            stockNode.put("cost", 0);
            stockNode.put("quantity", bucket.stock());
            stockNode.put("reserve", bucket.reserve());
            stockNode.put("intransit", bucket.inTransit());
            stockNode.put("available", StockLedger.available(bucket));
            out.set("stock", stockNode);
        }
        return out;
    }

    private void fillMoney(ObjectNode document, ArrayNode rows) {
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal reserved = BigDecimal.ZERO;
        BigDecimal shipped = BigDecimal.ZERO;
        for (JsonNode row : rows) {
            BigDecimal price = decimal(row, "price");
            BigDecimal discount = decimal(row, "discount");
            BigDecimal factor = BigDecimal.ONE.subtract(discount.movePointLeft(2));
            sum = sum.add(decimal(row, "quantity").multiply(price).multiply(factor));
            reserved = reserved.add(decimal(row, "reserve").multiply(price));
            shipped = shipped.add(decimal(row, "shipped").multiply(price));
        }
        document.put("sum", sum);
        document.put("reservedSum", reserved);
        document.put("shippedSum", shipped);
        if (!document.has("payedSum")) {
            document.put("payedSum", 0);
        }
    }

    private ObjectNode positionPage(String type, String id, Query query) {
        ObjectNode document = required(type, id);
        ArrayNode rows = positionsOf(document);
        ArrayNode pageRows = mapper.createArrayNode();
        for (int i = query.offset; i < rows.size() && pageRows.size() < query.limit; i++) {
            pageRows.add(presentPositionNode(type, document, (ObjectNode) rows.get(i), "stock".equals(query.fields)));
        }
        ObjectNode page = mapper.createObjectNode();
        page.set("meta", collectionMeta(href(type, id) + "/positions", type + "position", rows.size(), query.limit, query.offset));
        page.set("rows", pageRows);
        return page;
    }

    private ObjectNode page(String type, Query query) {
        List<ObjectNode> matched = new ArrayList<>();
        for (ObjectNode entity : bucket(type).values()) {
            if (query.matches(entity)) {
                matched.add(entity);
            }
        }
        matched.sort(query.comparator());
        ArrayNode rows = mapper.createArrayNode();
        for (int i = query.offset; i < matched.size() && rows.size() < query.limit; i++) {
            rows.add(present(type, matched.get(i), query, false));
        }
        ObjectNode page = mapper.createObjectNode();
        ObjectNode meta = collectionMeta(properties.publicBaseUrl() + "/api/remap/1.2/entity/" + type, type, matched.size(), query.limit, query.offset);
        if (query.offset + query.limit < matched.size()) {
            meta.put("nextHref", meta.get("href").asText() + "?offset=" + (query.offset + query.limit) + "&limit=" + query.limit);
        }
        page.set("meta", meta);
        page.set("context", context());
        page.set("rows", rows);
        return page;
    }

    private ArrayNode listOfType(String type) {
        ArrayNode rows = mapper.createArrayNode();
        for (ObjectNode entity : bucket(type).values()) {
            rows.add(present(type, entity, Query.none(), true));
        }
        return rows;
    }

    private ObjectNode assortment(Query query) {
        List<ObjectNode> matched = new ArrayList<>();
        matched.addAll(bucket("product").values());
        matched.addAll(bucket("variant").values());
        matched.removeIf(entity -> !query.matches(entity));
        ArrayNode rows = mapper.createArrayNode();
        for (int i = query.offset; i < matched.size() && rows.size() < query.limit; i++) {
            ObjectNode entity = matched.get(i);
            String type = bucket("variant").containsKey(entity.get("id").asText()) ? "variant" : "product";
            rows.add(present(type, entity, query, false));
        }
        ObjectNode page = mapper.createObjectNode();
        page.set("meta", collectionMeta(properties.publicBaseUrl() + "/api/remap/1.2/entity/assortment", "assortment", matched.size(), query.limit, query.offset));
        page.set("rows", rows);
        return page;
    }

    private ObjectNode metadata(String type) {
        ObjectNode metadata = mapper.createObjectNode();
        ArrayNode states = mapper.createArrayNode();
        for (ObjectNode state : bucket("state").values()) {
            if (type.equals(state.path("entityType").asText())) {
                states.add(present("state", state, Query.none(), true));
            }
        }
        metadata.set("states", states);
        metadata.set("meta", meta("metadata", type));
        return metadata;
    }

    private JsonNode stockReport(Query query, boolean byStore, boolean current) {
        List<ObjectNode> rows = new ArrayList<>();
        for (var entry : stock.snapshot().entrySet()) {
            String[] parts = entry.getKey().split("\\|", -1);
            ObjectNode row = reportRow(parts[0], parts[1].isEmpty() ? null : parts[1], entry.getValue(), byStore);
            if (query.includeZero || entry.getValue().stock().signum() != 0 || entry.getValue().reserve().signum() != 0) {
                rows.add(row);
            }
        }
        ArrayNode pageRows = mapper.createArrayNode();
        for (int i = query.offset; i < rows.size() && pageRows.size() < query.limit; i++) {
            pageRows.add(rows.get(i));
        }
        ObjectNode page = mapper.createObjectNode();
        String path = byStore ? "/report/stock/bystore" : "/report/stock/all";
        page.set("meta", collectionMeta(properties.publicBaseUrl() + "/api/remap/1.2" + path, "stock", rows.size(), query.limit, query.offset));
        page.set("rows", pageRows);
        return page;
    }

    private ArrayNode stockCurrent(Query query, boolean byStore) {
        ArrayNode rows = mapper.createArrayNode();
        String stockType = query.stockType == null ? "stock" : query.stockType;
        Instant since = query.changedSince;
        for (var entry : stock.snapshot().entrySet()) {
            StockLedger.Bucket bucket = entry.getValue();
            if (since != null && bucket.changedAt().isBefore(since)) {
                continue;
            }
            BigDecimal value = switch (stockType) {
                case "freeStock" -> StockLedger.freeStock(bucket);
                case "quantity" -> StockLedger.available(bucket);
                case "reserve" -> bucket.reserve();
                case "inTransit" -> bucket.inTransit();
                default -> bucket.stock();
            };
            if (since == null && !query.includeZero && value.signum() == 0) {
                continue;
            }
            String[] parts = entry.getKey().split("\\|", -1);
            ObjectNode row = mapper.createObjectNode();
            row.put("assortmentId", parts[0]);
            if (byStore) {
                row.put("storeId", parts[1]);
            }
            row.put(stockType, value);
            rows.add(row);
        }
        return rows;
    }

    private ObjectNode reportRow(String assortmentId, String storeId, StockLedger.Bucket bucket, boolean byStore) {
        ObjectNode entity = findAssortment(assortmentId);
        ObjectNode row = mapper.createObjectNode();
        row.set("meta", entity == null ? meta(assortmentId, "product") : entity.get("meta") == null
                ? meta(assortmentId, bucket("variant").containsKey(assortmentId) ? "variant" : "product")
                : entity.get("meta").deepCopy());
        if (entity != null && entity.get("meta") == null) {
            row.set("meta", meta(assortmentId, bucket("variant").containsKey(assortmentId) ? "variant" : "product"));
        }
        row.put("name", entity == null ? assortmentId : entity.path("name").asText());
        row.put("stock", bucket.stock());
        row.put("reserve", bucket.reserve());
        row.put("inTransit", bucket.inTransit());
        row.put("quantity", StockLedger.available(bucket));
        row.put("price", 0);
        row.put("salePrice", entity == null ? BigDecimal.ZERO : salePrice(entity));
        if (byStore && storeId != null && !storeId.isBlank()) {
            ArrayNode byStoreRows = mapper.createArrayNode();
            ObjectNode storeRow = mapper.createObjectNode();
            storeRow.set("meta", meta(storeId, "store"));
            storeRow.put("name", nameOf("store", storeId));
            storeRow.put("stock", bucket.stock());
            storeRow.put("reserve", bucket.reserve());
            storeRow.put("inTransit", bucket.inTransit());
            byStoreRows.add(storeRow);
            row.set("stockByStore", byStoreRows);
        }
        return row;
    }

    private ObjectNode findAssortment(String id) {
        if (bucket("product").containsKey(id)) {
            return bucket("product").get(id);
        }
        return bucket("variant").get(id);
    }

    private BigDecimal salePrice(ObjectNode entity) {
        JsonNode prices = entity.get("salePrices");
        if (prices != null && prices.isArray() && !prices.isEmpty()) {
            return prices.get(0).path("value").decimalValue();
        }
        return BigDecimal.ZERO;
    }

    private ObjectNode auditPage(Query query) {
        ArrayNode rows = mapper.createArrayNode();
        for (int i = query.offset; i < audit.size() && rows.size() < query.limit; i++) {
            rows.add(audit.get(i));
        }
        ObjectNode page = mapper.createObjectNode();
        page.set("meta", collectionMeta(properties.publicBaseUrl() + "/api/remap/1.2/audit", "audit", audit.size(), query.limit, query.offset));
        page.set("rows", rows);
        return page;
    }

    private void rememberAudit(String type, String id, String action) {
        ObjectNode row = mapper.createObjectNode();
        String auditId = UUID.randomUUID().toString();
        row.put("id", auditId);
        row.set("meta", meta(auditId, "audit"));
        row.put("moment", now());
        row.put("uid", "stub@example.com");
        row.put("eventType", action.toLowerCase(Locale.ROOT));
        ObjectNode entity = mapper.createObjectNode();
        entity.set("meta", meta(id, type));
        row.set("entity", entity);
        audit.add(row);
    }

    private void emit(String type, String id, String action, HttpServletRequest request) {
        List<String> disabled = new ArrayList<>();
        if (request != null) {
            Enumeration<String> headers = request.getHeaders("X-Lognex-WebHook-DisableByPrefix");
            while (headers != null && headers.hasMoreElements()) {
                disabled.add(headers.nextElement());
            }
        }
        ArrayNode events = mapper.createArrayNode();
        ObjectNode event = mapper.createObjectNode();
        event.set("meta", meta(id, type));
        event.put("action", action);
        event.put("accountId", SeedIds.ACCOUNT);
        events.add(event);
        List<JsonNode> ordered = WebhookBus.orderEvents(List.of(event), webhooks.disorder());
        for (ObjectNode subscription : bucket("webhook").values()) {
            if (!truth(subscription, "enabled")) {
                continue;
            }
            if (!type.equals(subscription.path("entityType").asText())) {
                continue;
            }
            if (!action.equals(subscription.path("action").asText())) {
                continue;
            }
            String url = subscription.path("url").asText();
            if (disabled.stream().anyMatch(url::startsWith)) {
                continue;
            }
            ObjectNode payload = mapper.createObjectNode();
            ObjectNode context = mapper.createObjectNode();
            context.set("meta", meta(UUID.randomUUID().toString(), "audit"));
            context.put("moment", now());
            context.put("uid", "stub@example.com");
            payload.set("auditContext", context);
            ArrayNode bodyEvents = mapper.createArrayNode();
            ordered.forEach(bodyEvents::add);
            payload.set("events", bodyEvents);
            webhooks.deliver(url, payload, UUID.randomUUID().toString());
        }
    }

    private ObjectNode employee() {
        ObjectNode employee = mapper.createObjectNode();
        employee.set("meta", meta(SeedIds.ACCOUNT, "employee"));
        employee.put("uid", "stub@example.com");
        employee.put("name", "Stub");
        return employee;
    }

    private ObjectNode context() {
        ObjectNode context = mapper.createObjectNode();
        ObjectNode employee = mapper.createObjectNode();
        employee.set("meta", meta(SeedIds.ACCOUNT, "employee"));
        context.set("employee", employee);
        return context;
    }

    private void expandRef(ObjectNode document, String field) {
        JsonNode ref = document.get(field);
        String id = idFromMeta(ref);
        String type = ref.path("meta").path("type").asText(null);
        if (id == null || type == null) {
            return;
        }
        ObjectNode target = bucket(type).get(id);
        if (target != null) {
            document.set(field, present(type, target, Query.none(), true));
        }
    }

    private ObjectNode required(String type, String id) {
        ObjectNode found = bucket(type).get(id);
        if (found == null) {
            throw RemapException.notFound("Сущность " + type + " " + id + " не найдена");
        }
        return found;
    }

    private LinkedHashMap<String, ObjectNode> bucket(String type) {
        return entities.computeIfAbsent(type, key -> new LinkedHashMap<>());
    }

    private void mergeWritable(ObjectNode target, JsonNode body) {
        for (String name : body.propertyNames()) {
            if (READ_ONLY.contains(name) || name.equals("positions") || name.startsWith("_")) {
                continue;
            }
            JsonNode value = body.get(name);
            if (value.isNull()) {
                target.remove(name);
            } else {
                target.set(name, value.deepCopy());
            }
        }
    }

    private void requireRef(JsonNode body, String field) {
        if (idFromMeta(body.get(field)) == null) {
            throw RemapException.precondition("Не заполнено обязательное поле " + field);
        }
    }

    private ObjectNode meta(String id, String type) {
        ObjectNode meta = mapper.createObjectNode();
        meta.put("href", href(type, id));
        meta.put("type", type);
        meta.put("mediaType", "application/json");
        return meta;
    }

    private ObjectNode collectionMeta(String href, String type, int size, int limit, int offset) {
        ObjectNode meta = mapper.createObjectNode();
        meta.put("href", href);
        meta.put("type", type);
        meta.put("mediaType", "application/json");
        meta.put("size", size);
        meta.put("limit", limit);
        meta.put("offset", offset);
        return meta;
    }

    private String href(String type, String id) {
        String collection = switch (type) {
            case "audit" -> "audit/" + id;
            case "employee" -> "context/employee";
            default -> "entity/" + type + "/" + id;
        };
        return properties.publicBaseUrl() + "/api/remap/1.2/" + collection;
    }

    private String nameOf(String type, String id) {
        ObjectNode entity = bucket(type).get(id);
        return entity == null ? id : entity.path("name").asText(id);
    }

    private static String idFromMeta(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.hasNonNull("id")) {
            return node.get("id").asText();
        }
        String href = node.path("meta").path("href").asText(null);
        if (href == null) {
            return null;
        }
        int slash = href.lastIndexOf('/');
        return slash < 0 ? null : href.substring(slash + 1);
    }

    private static String assortmentId(JsonNode position) {
        String id = idFromMeta(position.get("assortment"));
        if (id == null) {
            throw RemapException.precondition("У позиции не заполнен assortment");
        }
        return id;
    }

    private static String storeId(ObjectNode document) {
        String id = idFromMeta(document.get("store"));
        return id == null ? SeedIds.STORE : id;
    }

    private static boolean truth(ObjectNode node, String field) {
        return node.path(field).asBoolean(false);
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isNumber()) {
            return BigDecimal.ZERO;
        }
        return value.decimalValue();
    }

    private static String now() {
        return LocalDateTime.now().format(MOMENT);
    }

    public synchronized ObjectNode saveRaw(String type, ObjectNode entity) {
        bucket(type).put(entity.get("id").asText(), entity);
        return entity;
    }

    private record Query(int limit, int offset, String expand, String fields, String filter, String order,
                         String search, String stockType, boolean includeZero, Instant changedSince) {
        static Query none() {
            return new Query(1000, 0, null, null, null, null, null, "stock", false, null);
        }

        static Query from(HttpServletRequest request) {
            int limit = parseInt(request.getParameter("limit"), 1000);
            if (limit < 1 || limit > 1000) {
                throw RemapException.badRequest("limit должен быть от 1 до 1000");
            }
            int offset = parseInt(request.getParameter("offset"), 0);
            String changed = request.getParameter("changedSince");
            Instant since = null;
            if (changed != null && !changed.isBlank()) {
                since = LocalDateTime.parse(changed, MOMENT).atZone(ZoneId.systemDefault()).toInstant();
            }
            return new Query(limit, offset, request.getParameter("expand"), request.getParameter("fields"),
                    request.getParameter("filter"), request.getParameter("order"), request.getParameter("search"),
                    request.getParameter("stockType"), "zeroLines".equals(request.getParameter("include")), since);
        }

        boolean expands(String field) {
            return expand != null && expand.contains(field);
        }

        boolean matches(ObjectNode entity) {
            if (search != null && !entity.path("name").asText("").toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))) {
                return false;
            }
            if (filter == null || filter.isBlank()) {
                return !entity.path("archived").asBoolean(false) || filterMentionsArchived();
            }
            for (String part : filter.split(";")) {
                if (!matchOne(entity, part)) {
                    return false;
                }
            }
            return true;
        }

        private boolean filterMentionsArchived() {
            return filter != null && filter.contains("archived");
        }

        private boolean matchOne(ObjectNode entity, String expression) {
            String operator = operatorOf(expression);
            if (operator == null) {
                return true;
            }
            int index = expression.indexOf(operator);
            String field = expression.substring(0, index);
            String raw = expression.substring(index + operator.length());
            JsonNode value = entity.get(field);
            if ("syncId".equals(field) || "id".equals(field) || "name".equals(field) || "email".equals(field)
                    || "externalCode".equals(field) || "article".equals(field)) {
                return compareText(value == null ? "" : value.asText(), raw, operator);
            }
            if ("archived".equals(field) || "applicable".equals(field)) {
                return entity.path(field).asBoolean(false) == Boolean.parseBoolean(raw);
            }
            if ("updated".equals(field)) {
                return compareText(entity.path("updated").asText(), raw, operator);
            }
            return true;
        }

        private static boolean compareText(String actual, String expected, String operator) {
            return switch (operator) {
                case "!=" -> !actual.equals(expected);
                case "~" -> actual.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
                case ">=" -> actual.compareTo(expected) >= 0;
                case "<=" -> actual.compareTo(expected) <= 0;
                case ">" -> actual.compareTo(expected) > 0;
                case "<" -> actual.compareTo(expected) < 0;
                default -> actual.equals(expected);
            };
        }

        private static String operatorOf(String expression) {
            for (String candidate : List.of(">=", "<=", "!=", "~=", "=~", "~", ">", "<", "=")) {
                if (expression.contains(candidate)) {
                    return candidate;
                }
            }
            return null;
        }

        Comparator<ObjectNode> comparator() {
            if (order == null || order.isBlank()) {
                return Comparator.comparing(node -> node.path("name").asText(""));
            }
            String[] parts = order.split(",");
            boolean desc = parts.length > 1 && "desc".equalsIgnoreCase(parts[1]);
            Comparator<ObjectNode> comparator = Comparator.comparing(node -> node.path(parts[0]).asText(""));
            return desc ? comparator.reversed() : comparator;
        }

        private static int parseInt(String raw, int fallback) {
            if (raw == null || raw.isBlank()) {
                return fallback;
            }
            return Integer.parseInt(raw);
        }
    }
}
