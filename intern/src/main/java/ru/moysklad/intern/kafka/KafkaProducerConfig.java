package ru.moysklad.intern.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import ru.moysklad.intern.dto.kafka.ProcessingResultMessage;
import ru.moysklad.intern.dto.kafka.SalesCommand;
import ru.moysklad.intern.dto.kafka.StockChangeCommand;

import java.util.HashMap;
import java.util.Map;

/**
 * Типизированные {@link KafkaTemplate} для команд и результатов.
 * Sales-шаблоны доступны студенту; публикацию SalesCommand реализует студент.
 */
@Configuration
public class KafkaProducerConfig {

    @Bean
    ProducerFactory<String, StockChangeCommand> stockChangeProducerFactory(
            KafkaProperties kafkaProperties,
            KafkaConnectionDetails connectionDetails
    ) {
        return new DefaultKafkaProducerFactory<>(jsonProducerConfigs(kafkaProperties, connectionDetails));
    }

    @Bean
    KafkaTemplate<String, StockChangeCommand> stockChangeKafkaTemplate(
            ProducerFactory<String, StockChangeCommand> stockChangeProducerFactory
    ) {
        return new KafkaTemplate<>(stockChangeProducerFactory);
    }

    @Bean
    ProducerFactory<String, SalesCommand> salesCommandProducerFactory(
            KafkaProperties kafkaProperties,
            KafkaConnectionDetails connectionDetails
    ) {
        return new DefaultKafkaProducerFactory<>(jsonProducerConfigs(kafkaProperties, connectionDetails));
    }

    /**
     * Шаблон для публикации SalesCommand. Используется студенческой реализацией SalesKafkaProducer.
     */
    @Bean
    KafkaTemplate<String, SalesCommand> salesCommandKafkaTemplate(
            ProducerFactory<String, SalesCommand> salesCommandProducerFactory
    ) {
        return new KafkaTemplate<>(salesCommandProducerFactory);
    }

    @Bean
    ProducerFactory<String, ProcessingResultMessage> processingResultProducerFactory(
            KafkaProperties kafkaProperties,
            KafkaConnectionDetails connectionDetails
    ) {
        return new DefaultKafkaProducerFactory<>(jsonProducerConfigs(kafkaProperties, connectionDetails));
    }

    @Bean
    KafkaTemplate<String, ProcessingResultMessage> processingResultKafkaTemplate(
            ProducerFactory<String, ProcessingResultMessage> processingResultProducerFactory
    ) {
        return new KafkaTemplate<>(processingResultProducerFactory);
    }

    private Map<String, Object> jsonProducerConfigs(
            KafkaProperties kafkaProperties,
            KafkaConnectionDetails connectionDetails
    ) {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildProducerProperties());
        // ServiceConnection / KafkaConnectionDetails перекрывают spring.kafka.bootstrap-servers
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionDetails.getProducer().getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        props.put(JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        return props;
    }
}
