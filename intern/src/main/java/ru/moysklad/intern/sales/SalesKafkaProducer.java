package ru.moysklad.intern.sales;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.dto.kafka.SalesCommand;
import ru.moysklad.intern.kafka.KafkaTopicProperties;

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

    private final KafkaTemplate<String, SalesCommand> kafkaTemplate;
    private final KafkaTopicProperties topics;

    public SalesKafkaProducer(KafkaTemplate<String, SalesCommand> kafkaTemplate, KafkaTopicProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    public void publish(SalesCommand command) {
        kafkaTemplate.send(topics.getSalesCommands(), command.requestId().toString(), command);
    }
}
