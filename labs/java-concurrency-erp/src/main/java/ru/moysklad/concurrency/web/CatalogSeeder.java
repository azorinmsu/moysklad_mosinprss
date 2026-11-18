package ru.moysklad.concurrency.web;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.moysklad.concurrency.inventory.InventoryService;

@Component
public class CatalogSeeder implements ApplicationRunner {

    private final InventoryService inventory;

    public CatalogSeeder(InventoryService inventory) {
        this.inventory = inventory;
    }

    @Override
    public void run(ApplicationArguments args) {
        inventory.acceptOpeningBalance("paper-a4", "main", 100);
        inventory.acceptOpeningBalance("paper-a4", "secondary", 40);
        inventory.acceptOpeningBalance("pen-blue", "main", 25);
    }
}
