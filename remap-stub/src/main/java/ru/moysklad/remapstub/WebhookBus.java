package ru.moysklad.remapstub;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;

@Component
public class WebhookBus {

    public record Delivery(String id, String requestId, String url, String body, int status, String error, Instant moment) {
    }

    private final List<Delivery> deliveries = new CopyOnWriteArrayList<>();
    private volatile boolean disorder;

    public boolean disorder() {
        return disorder;
    }

    public void setDisorder(boolean disorder) {
        this.disorder = disorder;
    }

    public List<Delivery> deliveries() {
        return List.copyOf(deliveries);
    }

    public void clear() {
        deliveries.clear();
        disorder = false;
    }

    public Delivery deliver(String url, ObjectNode payload, String requestId) {
        String id = UUID.randomUUID().toString();
        int status = 0;
        String error = null;
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .timeout(java.time.Duration.ofMillis(1500))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();
            java.net.http.HttpResponse<String> response = java.net.http.HttpClient.newHttpClient()
                    .send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            status = response.statusCode();
        } catch (Exception exception) {
            error = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }
        Delivery delivery = new Delivery(id, requestId, url, payload.toString(), status, error, Instant.now());
        deliveries.add(delivery);
        return delivery;
    }

    public Delivery redeliver(String deliveryId, tools.jackson.databind.ObjectMapper mapper) {
        Delivery existing = deliveries.stream()
                .filter(item -> item.id.equals(deliveryId))
                .findFirst()
                .orElseThrow(() -> RemapException.notFound("Доставка вебхука не найдена"));
        try {
            JsonNode payload = mapper.readTree(existing.body);
            return deliver(existing.url, (ObjectNode) payload, existing.requestId);
        } catch (RemapException exception) {
            throw exception;
        } catch (Exception exception) {
            throw RemapException.badRequest("Не удалось повторить доставку");
        }
    }

    public static List<JsonNode> orderEvents(List<JsonNode> events, boolean disorder) {
        if (!disorder || events.size() < 2) {
            return events;
        }
        List<JsonNode> copy = new ArrayList<>(events);
        java.util.Collections.reverse(copy);
        return copy;
    }
}
