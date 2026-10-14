package ru.moysklad.intern.stockchange;

import ru.moysklad.intern.dto.kafka.StockChangeCommand;

/**
 * Запуск обработки команды изменения остатков (Camunda-процесс на этапе 4).
 */
public interface StockChangeProcessLauncher {

    void launch(StockChangeCommand command);
}
