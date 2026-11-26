package ru.moysklad.concurrency.jobs;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Фоновая обработка заказов в пуле потоков.
 * Контекст организации одной задачи не виден следующей задаче на том же потоке.
 */
@Service
public class OrderEventProcessor {

    private final ThreadLocal<String> organizationId = new ThreadLocal<>();
    private final List<String> audit = Collections.synchronizedList(new ArrayList<>());

    public void processOrder(String organization, String orderId) {
        organizationId.set(organization);
        try {
            audit.add(organizationId.get() + ":" + orderId);
        } finally {
            organizationId.remove();
        }
    }

    public void runMaintenance() {
        String current = organizationId.get();
        audit.add(current == null ? "MAINTENANCE" : "LEAK:" + current);
    }

    public List<String> audit() {
        return List.copyOf(audit);
    }
}
