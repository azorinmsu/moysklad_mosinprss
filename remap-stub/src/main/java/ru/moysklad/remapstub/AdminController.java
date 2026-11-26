package ru.moysklad.remapstub;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class AdminController {

    private final SeedService seed;
    private final FaultRegistry faults;
    private final WebhookBus webhooks;
    private final RemapEngine engine;
    private final ObjectMapper mapper;

    public AdminController(SeedService seed, FaultRegistry faults, WebhookBus webhooks, RemapEngine engine, ObjectMapper mapper) {
        this.seed = seed;
        this.faults = faults;
        this.webhooks = webhooks;
        this.engine = engine;
        this.mapper = mapper;
    }

    @PostMapping("/api/stub/reset")
    public ObjectNode reset() {
        faults.clear();
        webhooks.clear();
        seed.reload();
        return ok("Состояние сброшено к начальным данным");
    }

    @PostMapping("/api/stub/seed")
    public ObjectNode seed() {
        return reset();
    }

    @GetMapping("/api/stub/faults")
    public List<FaultRegistry.Rule> faults() {
        return faults.rules();
    }

    @PostMapping("/api/stub/faults")
    public FaultRegistry.Rule addFault(@RequestBody JsonNode body) {
        return faults.add(
                body.path("method").asText("GET"),
                body.path("pathPattern").asText("/api/remap/1.2/**"),
                FaultRegistry.Type.valueOf(body.path("faultType").asText()),
                body.path("httpStatus").asInt(500),
                body.path("code").asInt(0),
                body.path("delayMs").asLong(0),
                body.path("count").asInt(1));
    }

    @DeleteMapping("/api/stub/faults")
    public ObjectNode clearFaults() {
        faults.clear();
        return ok("Правила сбоев очищены");
    }

    @PostMapping("/api/stub/unavailable")
    public ObjectNode unavailable(@RequestBody JsonNode body) {
        faults.setUnavailable(body.path("enabled").asBoolean(true));
        return ok(faults.unavailable() ? "API недоступен" : "API доступен");
    }

    @PostMapping("/api/stub/stock")
    public ObjectNode stock(@RequestBody JsonNode body) {
        return engine.putStock(
                body.path("assortmentId").asText(),
                body.path("storeId").asText(SeedIds.STORE),
                decimal(body, "stock"),
                decimal(body, "reserve"),
                decimal(body, "inTransit"));
    }

    @GetMapping("/api/stub/webhook-deliveries")
    public List<WebhookBus.Delivery> deliveries() {
        return webhooks.deliveries();
    }

    @PostMapping("/api/stub/webhooks/redeliver")
    public WebhookBus.Delivery redeliver(@RequestBody JsonNode body) {
        return webhooks.redeliver(body.path("deliveryId").asText(), mapper);
    }

    @PostMapping("/api/stub/webhooks/disorder")
    public ObjectNode disorder(@RequestBody JsonNode body) {
        webhooks.setDisorder(body.path("enabled").asBoolean(true));
        return ok(webhooks.disorder() ? "Следующая пачка событий уйдёт в обратном порядке" : "Порядок событий обычный");
    }

    @PostMapping("/api/stub/webhooks/emit")
    public WebhookBus.Delivery emit(@RequestBody JsonNode body) {
        ArrayNode events = (ArrayNode) body.get("events");
        List<JsonNode> ordered = new ArrayList<>();
        events.forEach(ordered::add);
        ordered = WebhookBus.orderEvents(ordered, webhooks.disorder());
        ObjectNode payload = mapper.createObjectNode();
        ArrayNode eventNodes = mapper.createArrayNode();
        for (JsonNode event : ordered) {
            ObjectNode node = mapper.createObjectNode();
            ObjectNode meta = mapper.createObjectNode();
            meta.put("href", "http://localhost/api/remap/1.2/entity/" + event.path("type").asText() + "/" + event.path("id").asText());
            meta.put("type", event.path("type").asText());
            meta.put("mediaType", "application/json");
            node.set("meta", meta);
            node.put("action", event.path("action").asText("UPDATE"));
            node.put("accountId", SeedIds.ACCOUNT);
            eventNodes.add(node);
        }
        payload.set("events", eventNodes);
        ObjectNode context = mapper.createObjectNode();
        context.put("uid", "stub@example.com");
        payload.set("auditContext", context);
        return webhooks.deliver(body.path("url").asText(), payload, UUID.randomUUID().toString());
    }

    private ObjectNode ok(String message) {
        ObjectNode node = mapper.createObjectNode();
        node.put("message", message);
        return node;
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        return node.has(field) ? node.get(field).decimalValue() : BigDecimal.ZERO;
    }
}
