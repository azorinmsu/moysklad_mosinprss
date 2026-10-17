package ru.moysklad.intern.sales;

import ru.moysklad.intern.dto.kafka.SalesCommand;

public interface SalesProcessLauncher {
    void launch(SalesCommand command);
}
