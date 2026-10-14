package ru.moysklad.intern.sales;

import org.springframework.stereotype.Component;
import ru.moysklad.intern.dto.kafka.SalesCommand;

/**
 * Публикация команд реализации в топик {@code sales.commands}.
 *
 * <p><b>TODO (студент):</b> внедрить {@code KafkaTemplate<String, SalesCommand>}
 * (бин уже объявлен в инфраструктуре) и отправить сообщение с ключом requestId.
 *
 * <p>Не копируйте готовую реализацию из {@code StockChangeKafkaProducer} без понимания:
 * нужно самостоятельно выбрать topic, key и обработать ошибки публикации.
 */
@Component
public class SalesKafkaProducer {

    // TODO(student): KafkaTemplate<String, SalesCommand> + KafkaTopicProperties

    /**
     * TODO(student): опубликовать SalesCommand в Kafka.
     */
    public void publish(SalesCommand command) {
        throw new UnsupportedOperationException("TODO: реализовать SalesKafkaProducer.publish");
    }
}
