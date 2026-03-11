package ru.yandex.practicum.consumer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.yandex.practicum.ewm.stats.avro.UserActionAvro;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Properties;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "analyzer.kafka")
public class KafkaConsumerConfig {

    private Properties userActionConsumerProps;
    private Properties eventSimilarityConsumerProps;
    private String userActionsTopic;
    private String eventsSimilarityTopic;

    private Consumer<Long, UserActionAvro> userActionsConsumer;
    private Consumer<Long, EventSimilarityAvro> eventsSimilarityConsumer;

    @PostConstruct
    public void init() {
        log.info("=== ЗАГРУЖЕННАЯ КОНФИГУРАЦИЯ ===");
        log.info("userActionConsumerProps: {}", userActionConsumerProps);
        log.info("eventSimilarityConsumerProps: {}", eventSimilarityConsumerProps);
        log.info("userActionsTopic: {}", userActionsTopic);
        log.info("eventsSimilarityTopic: {}", eventsSimilarityTopic);
        log.info("=================================");
    }

    public Consumer<Long, UserActionAvro> getUserActionsConsumer() {
        if (userActionsConsumer == null) {
            log.info("Создаем consumer для user actions");
            userActionsConsumer = new KafkaConsumer<>(userActionConsumerProps);
        }
        return userActionsConsumer;
    }

    public Consumer<Long, EventSimilarityAvro> getEventsSimilarityConsumer() {
        if (eventsSimilarityConsumer == null) {
            log.info("Создаем consumer для events similarity");
            eventsSimilarityConsumer = new KafkaConsumer<>(eventSimilarityConsumerProps);
        }
        return eventsSimilarityConsumer;
    }

    public String getUserActionsTopic() {
        return userActionsTopic;
    }

    public String getEventsSimilarityTopic() {
        return eventsSimilarityTopic;
    }

    @PreDestroy
    public void stop() {
        if (userActionsConsumer != null) {
            userActionsConsumer.close();
        }
        if (eventsSimilarityConsumer != null) {
            eventsSimilarityConsumer.close();
        }
    }

    public void setUserActionConsumerProps(Properties userActionConsumerProps) {
        this.userActionConsumerProps = userActionConsumerProps;
    }

    public void setEventSimilarityConsumerProps(Properties eventSimilarityConsumerProps) {
        this.eventSimilarityConsumerProps = eventSimilarityConsumerProps;
    }

    public void setUserActionsTopic(String userActionsTopic) {
        this.userActionsTopic = userActionsTopic;
    }

    public void setEventsSimilarityTopic(String eventsSimilarityTopic) {
        this.eventsSimilarityTopic = eventsSimilarityTopic;
    }
}