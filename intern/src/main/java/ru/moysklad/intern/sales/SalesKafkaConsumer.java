package ru.moysklad.intern.sales;

import org.springframework.stereotype.Component;
import ru.moysklad.intern.dto.kafka.SalesCommand;

/**
 * Чтение команд реализации из топика {@code sales.commands}.
 *
 * <p><b>TODO (студент):</b>
 * <ul>
 *   <li>добавить {@code @KafkaListener} на topic {@code sales.commands};</li>
 *   <li>десериализовать {@link SalesCommand};</li>
 *   <li>обеспечить идемпотентность по requestId;</li>
 *   <li>сохранить заявку со статусом PENDING;</li>
 *   <li>не запускать Camunda из listener напрямую по таймеру —
 *       timer обрабатывает уже сохранённые PENDING-заявки.</li>
 * </ul>
 *
 * <p>Рабочую реализацию StockChangeKafkaConsumer не переносите сюда целиком:
 * у Sales другой жизненный цикл (PENDING → периодический захват).
 */
@Component
public class SalesKafkaConsumer {

    // TODO(student): зависимости и @KafkaListener

    /**
     * TODO(student): обработать входящую SalesCommand.
     */
    public void consume(SalesCommand command) {
        throw new UnsupportedOperationException("TODO: реализовать SalesKafkaConsumer.consume");
    }
}
