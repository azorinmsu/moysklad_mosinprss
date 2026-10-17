package ru.moysklad.intern.sales;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    // TODO(student): KafkaTemplate<String, SalesCommand> + KafkaTopicProperties

    // я думаю логгер для ошибок лишним не будет
    private static final Logger log = LoggerFactory.getLogger(SalesKafkaProducer.class);
    private final KafkaTemplate<String, SalesCommand> kafkaTemplate;
    private final KafkaTopicProperties topics;

    public SalesKafkaProducer(KafkaTemplate<String, SalesCommand> kafkaTemplate, KafkaTopicProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    /**
     * TODO(student): опубликовать SalesCommand в Kafka.
     */
    public void publish(SalesCommand command) {
        try {
            kafkaTemplate.send(topics.getSalesCommands(), command.requestId().toString(), command);
        } catch (RuntimeException e) {
            // на всякий
            log.error("Ошибка при создании публикации SalesCommand: ", e);
            throw new RuntimeException("Не удалось опубликовать SalesCommand: ", e);
        }
    }
}
