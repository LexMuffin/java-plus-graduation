package ru.yandex.practicum.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aggregator.kafka")
public class AggregatorKafkaProperties {
    private Properties producerProps;
    private Properties consumerProps;
    private String userActionsTopic;
    private String eventsSimilarityTopic;
}