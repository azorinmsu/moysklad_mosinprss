package ru.moysklad.intern.stockchange;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.moysklad.intern.dto.kafka.StockChangeCommand;
import ru.moysklad.intern.kafka.KafkaTopicProperties;

@Component
public class StockChangeKafkaProducer {

    private final KafkaTemplate<String, StockChangeCommand> kafkaTemplate;
    private final KafkaTopicProperties topics;

    public StockChangeKafkaProducer(
            KafkaTemplate<String, StockChangeCommand> kafkaTemplate,
            KafkaTopicProperties topics
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    public void publish(StockChangeCommand command) {
        kafkaTemplate.send(topics.getStockCommands(), command.requestId().toString(), command);
    }
}
