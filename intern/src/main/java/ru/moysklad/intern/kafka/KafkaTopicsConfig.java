package ru.moysklad.intern.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaTopicsConfig {

    @Bean
    NewTopic stockCommandsTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.getStockCommands()).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic salesCommandsTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.getSalesCommands()).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic processingResultsTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.getProcessingResults()).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic processingResultsDlqTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.getProcessingResultsDlq()).partitions(1).replicas(1).build();
    }
}
