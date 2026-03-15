package ru.yandex.practicum.consumer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "analyzer.kafka")
public class KafkaConsumerConfig {
    private Properties userActionConsumerProps;
    private Properties eventSimilarityConsumerProps;
    private String userActionsTopic;
    private String eventsSimilarityTopic;

    public void logConfig() {
        log.info("=== КОНФИГУРАЦИЯ KAFKA ===");
        log.info("userActionConsumerProps: {}", userActionConsumerProps);
        log.info("eventSimilarityConsumerProps: {}", eventSimilarityConsumerProps);
        log.info("userActionsTopic: {}", userActionsTopic);
        log.info("eventsSimilarityTopic: {}", eventsSimilarityTopic);
    }
}